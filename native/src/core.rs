//! JSON facade over the engine. Every address is emitted as a "0x.." hex string
//! so Kotlin never has to deal with unsigned 64-bit numbers.

use ghidra_decompiler::{CallEdge, Instruction, Program};
use serde_json::{json, Value};
use std::path::Path;

pub type Res = Result<Value, String>;

fn e<E: std::fmt::Debug>(err: E) -> String {
    format!("{err:?}")
}

fn addr(a: u64) -> String {
    format!("0x{a:x}")
}

fn hex(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{b:02x}")).collect()
}

fn insn(i: &Instruction) -> Value {
    json!({
        "address": addr(i.address),
        "length": i.length,
        "bytes": hex(&i.bytes),
        "mnemonic": i.mnemonic,
        "operands": i.operands,
    })
}

pub struct Session {
    program: Program,
    call_graph: Option<Vec<CallEdge>>,
}

impl Session {
    /// Opens a binary read-only. `language` = None lets the engine detect it from the header.
    pub fn open(path: &str, language: Option<&str>) -> Result<Session, String> {
        let program = Program::open(Path::new(path), language).map_err(e)?;
        Ok(Session { program, call_graph: None })
    }

    pub fn info(&mut self) -> Res {
        let function_count = self.program.functions().map_err(e)?.len();
        let sections: Vec<Value> = self
            .program
            .code_sections()
            .iter()
            .map(|r| json!({"start": addr(r.start), "end": addr(r.end)}))
            .collect();
        Ok(json!({
            "language": self.program.language_id(),
            "description": self.program.description(),
            "entry": self.program.entry_point().map(addr),
            "code_sections": sections,
            "function_count": function_count,
        }))
    }

    pub fn functions(&mut self, offset: usize, limit: usize) -> Res {
        let mut all = self.program.functions().map_err(e)?;
        all.sort_by_key(|f| f.address);
        let total = all.len();
        let items: Vec<Value> = all
            .into_iter()
            .skip(offset)
            .take(limit)
            .map(|f| {
                json!({
                    "name": f.name,
                    "symbol": f.symbol,
                    "address": addr(f.address),
                    "source": format!("{:?}", f.source),
                })
            })
            .collect();
        Ok(json!({"total": total, "offset": offset, "items": items}))
    }

    pub fn disassemble(&mut self, address: u64, count: usize) -> Res {
        let list = self.program.disassemble(address, count).map_err(e)?;
        Ok(json!({"instructions": list.iter().map(insn).collect::<Vec<_>>()}))
    }

    pub fn disassemble_function(&mut self, address: u64) -> Res {
        let size = self.program.function_size(address).map_err(e)?;
        let rep = self.program.disassemble_range_recovering(address, size).map_err(e)?;
        Ok(json!({
            "address": addr(address),
            "function_size": size,
            "requested_bytes": rep.requested_bytes,
            "decoded_bytes": rep.decoded_bytes,
            "instructions": rep.instructions.iter().map(insn).collect::<Vec<_>>(),
            "gaps": rep.gaps.iter().map(|g| json!({"address": addr(g.address), "length": g.length})).collect::<Vec<_>>(),
        }))
    }

    pub fn decompile(&mut self, address: u64) -> Res {
        let name = self.program.find_function_at(address).map_err(e)?.map(|f| f.name);
        let c = self.program.decompile(address).map_err(e)?;
        Ok(json!({"address": addr(address), "name": name, "c": c}))
    }

    pub fn strings(&mut self, offset: usize, limit: usize) -> Res {
        let all = self.program.strings().map_err(e)?;
        let total = all.len();
        let items: Vec<Value> = all
            .into_iter()
            .skip(offset)
            .take(limit)
            .map(|s| json!({"address": addr(s.address), "value": s.value}))
            .collect();
        Ok(json!({"total": total, "offset": offset, "items": items}))
    }

    pub fn data_symbols(&mut self, offset: usize, limit: usize) -> Res {
        let all = self.program.data_symbols().map_err(e)?;
        let total = all.len();
        let items: Vec<Value> = all
            .into_iter()
            .skip(offset)
            .take(limit)
            .map(|d| json!({"name": d.name, "symbol": d.symbol, "address": addr(d.address), "size": d.size}))
            .collect();
        Ok(json!({"total": total, "offset": offset, "items": items}))
    }

    pub fn relocations(&mut self, offset: usize, limit: usize) -> Res {
        let all: Vec<Value> = self
            .program
            .relocations()
            .map(|r| json!({"address": addr(r.address), "kind": r.kind, "symbol": r.symbol, "addend": r.addend}))
            .collect();
        let total = all.len();
        let items: Vec<Value> = all.into_iter().skip(offset).take(limit).collect();
        Ok(json!({"total": total, "offset": offset, "items": items}))
    }

    pub fn references_from(&mut self, address: u64) -> Res {
        let refs: Vec<Value> = self
            .program
            .references_from(address)
            .iter()
            .map(|r| json!({"from": addr(r.from), "to": addr(r.to), "kind": r.kind, "is_primary": r.is_primary}))
            .collect();
        Ok(json!({"references": refs}))
    }

    /// Direct-call relations of the function at `address`. The whole-program call graph is
    /// computed once per session (it costs a flow analysis per function) and then cached.
    pub fn call_relations(&mut self, address: u64) -> Res {
        if self.call_graph.is_none() {
            let g = self.program.call_graph().map_err(e)?;
            self.call_graph = Some(g);
        }
        let edges: Vec<(u64, u64, u64)> = self
            .call_graph
            .as_ref()
            .map(|g| g.iter().map(|x| (x.caller, x.callee, x.call_site)).collect())
            .unwrap_or_default();
        let mut callers = Vec::new();
        let mut callees = Vec::new();
        for (caller, callee, site) in edges {
            if callee == address {
                callers.push(self.edge_json(caller, site));
            }
            if caller == address {
                callees.push(self.edge_json(callee, site));
            }
        }
        Ok(json!({"address": addr(address), "callers": callers, "callees": callees}))
    }

    fn edge_json(&self, other: u64, call_site: u64) -> Value {
        let name = self.program.find_function_at(other).ok().flatten().map(|f| f.name);
        json!({"address": addr(other), "name": name, "call_site": addr(call_site)})
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// The test binary itself is a real ELF on Linux, so this exercises the real engine
    /// end to end without shipping fixture files.
    #[test]
    fn opens_own_executable_and_lists_functions() {
        let exe = std::env::current_exe().unwrap();
        let mut s = Session::open(exe.to_str().unwrap(), None).expect("open");
        let info = s.info().expect("info");
        assert!(info["function_count"].as_u64().unwrap() > 0, "no functions found");
        let page = s.functions(0, 5).expect("functions");
        let first = page["items"][0]["address"].as_str().unwrap().trim_start_matches("0x").to_string();
        let a = u64::from_str_radix(&first, 16).unwrap();
        let d = s.disassemble_function(a).expect("disassemble_function");
        assert!(d["instructions"].is_array());
    }
}
