package tech.riadh.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
	private final Map<String, Object> fields = new HashMap<>();
	private LoxClass loxClass;

	LoxInstance(LoxClass loxClass) {
		this.loxClass = loxClass;
	}

	Object get(Token name) {
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}
		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}

	void set(Token name, Object value) {
		fields.put(name.lexeme, value);
	}

	@Override
	public String toString() {
		return loxClass.name + " instance";
	}
}
