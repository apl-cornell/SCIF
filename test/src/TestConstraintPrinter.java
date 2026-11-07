import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import typecheck.ConstraintPrinter;

public class TestConstraintPrinter {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // principals of the contract being checked, and of another one
            "s11.C.this | this",
            "s11.C.any | any",
            "s11.C.TOP | this",
            "s11.D.this | D.this",
            "s11.D.any | D.any",
            // labels of fields, locals, and locals in nested scopes
            "s11.C.secret..lbl | label(secret)",
            "s11.C.f.x..lbl | label(x)",
            "s11.C.f.tryL13C9s11.e..lbl | label(e)",
            "s11.D.x..lbl | label(D.x)",
            // principal-valued variables, members, and versions
            "s11.C.f.sender | sender",
            "s11.C.sink | sink",
            "s11.C.f.tryL13C9s11.e.owner | e.owner",
            "s11.C.f.owner..v2 | owner#2",
            // labels of a method's signature and the locks of its body
            "s11.C.f.extpc | external begin label of f",
            "s11.C.f.inpc | internal begin label of f",
            "s11.C.f.gamma | reentrancy lock label of f",
            "s11.C.f.returnV | label of the return value of f",
            "s11.C.f.ILK | lock on entry to f",
            "s11.C.f.rtn.lk | lock at end of f",
            // control flow at program points
            "s11.C.f.PC | pc at start of f",
            "s11.C.f.guardBlockL9C9s11.CallL10C15s11.PC | pc at call 10:15",
            "s11.C.f.guardBlockL9C9s11.GuardBlockL9C9s11.PC.next | pc after lock block 9:9",
            "s11.C.f.CallL23C13s11.LK | lock after call 23:13",
            // values of expressions
            "s11.C.f.SubscriptL10C18s11 | element read 10:18",
            "s11.C.f.tryL13C9s11.structL16C22s11 | member read 16:22",
            "s11.C.f.NewL11C16s11..new | contract created by new 11:16",
            "s11.C.f.NameL7C14s11 | name 7:14",
            "s11.C.f.binL8C43s11 | operation 8:43",
            // types
            "global.uint | uint",
            // anything else is left alone
            "something.else | something.else",
    })
    void testName(String name, String expected) {
        assertEquals(expected, ConstraintPrinter.name(name, "C"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "s11.C.f.x..lbl<=s11.C.this | label(x) => this",
            "s11.C.f.x..lbl>=s11.C.this | this => label(x)",
            "s11.C.f.x..lbl==s11.C.any | label(x) = any",
            "(s11.C.m..lbl)join(s11.C.f.k..lbl)==s11.C.f.SubscriptL9C18s11"
                    + " | label(m) ⨆ label(k) = element read 9:18",
            "(s11.C.f.PC)join(s11.C.this)join(s11.C.f.x..lbl)<=s11.C.f.CallL10C15s11.PC"
                    + " | pc at start of f ⨆ this ⨆ label(x) => pc at call 10:15",
            "((s11.C.a)join(s11.C.b))meet(s11.C.c)<=s11.C.this | (a ⨆ b) ⨅ c => this",
            "(s11.C.a)meet((s11.C.b)join(s11.C.c))<=s11.C.this | a ⨅ (b ⨆ c) => this",
            // not in SHErrLoc's form: left alone
            "(s11.C.a<=s11.C.b | (s11.C.a<=s11.C.b",
            "a <= b; c | a <= b; c",
    })
    void testConstraint(String constraint, String expected) {
        assertEquals(expected, ConstraintPrinter.constraint(constraint, "C"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "s11.C.f.NameL7C9s11==global.uint | type of name 7:9 = uint",
            "s11.C.f.BinOpL7C17s11<=s11.C.f.NameL7C25s11 | type of operation 7:17 <= type of name 7:25",
            "s11.C.f.BinOpL7C17s11>=global.uint | type of operation 7:17 >= uint",
            "s11.C.s0==s11.C.f.NameL10C9s11 | s0 = type of name 10:9",
    })
    void testTypeConstraint(String constraint, String expected) {
        assertEquals(expected, ConstraintPrinter.typeConstraint(constraint, "C"));
    }
}
