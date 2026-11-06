# Information Flow Control

SCIF labels every value and every function with its *integrity*, which describes how trustworthy the information is, based on who could have influenced it. The compiler rejects any program in which less trusted information can affect more trusted state, unless the code explicitly endorses that information.

Because contract state on a public blockchain is visible to everyone, SCIF does not track confidentiality and tracks only integrity.

## Principals

A *principal* is an entity whose trust a program reasons about, such as an account, a contract, or the caller of a function. SCIF has three built-in principals:

- `this` is the current contract. From the contract's own point of view, it is the most trusted principal.
- `sender` is the caller of the current function.
- `any` is the least trusted principal and stands for anyone.

A `final address` variable can also be used as a principal, so a label can name an account that is only known at run time:

```scif
void credit(final address user, uint{user} amount) {
    ...
}
```

Here `amount` is labeled `{user}`: it is trusted by the account passed as `user`.

## Labels

A *label* is written in braces after a type. The declaration `uint{this} x` states that `x` holds information trusted by the contract itself, so only information the contract trusts can be stored in `x`.

Labels are ordered by trust. Information may flow from a more trusted label to a less trusted one, but never in the other direction:

<figure style="margin: 24px 0; text-align: center;">
<svg viewBox="0 0 360 200" style="display: block; margin: 0 auto; width: 100%; max-width: 360px; height: auto;" role="img" aria-label="The trust ordering of labels: this at the top, sender and user in the middle, any at the bottom. Arrows point in the direction information may flow.">
<defs>
<marker id="ifc-flow-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="currentColor"/></marker>
</defs>
<g stroke="currentColor" stroke-width="1.5" opacity="0.55" marker-end="url(#ifc-flow-arrow)">
<line x1="168" y1="40" x2="114" y2="84"/>
<line x1="192" y1="40" x2="246" y2="84"/>
<line x1="114" y1="114" x2="168" y2="158"/>
<line x1="246" y1="114" x2="192" y2="158"/>
</g>
<g fill="currentColor" text-anchor="middle" font-size="16" style="font-family: var(--vp-font-family-mono);">
<text x="180" y="30">this</text>
<text x="100" y="104">sender</text>
<text x="260" y="104">user</text>
<text x="180" y="178">any</text>
</g>
<g fill="currentColor" font-size="12" opacity="0.7">
<text x="0" y="30">more trusted</text>
<text x="0" y="178">less trusted</text>
</g>
</svg>
</figure>

`this` flows to every label, and every label flows to `any`. Other principals, such as `sender` and `user`, are incomparable: information cannot flow between them in either direction.

An assignment `x = e` is accepted only if the label of `e` flows to the label of `x`. In the following contract, the parameter `input` of the public function `update` is labeled `{sender}` by default:

```scif
contract Labels {
    uint{this} trusted;
    uint{any} untrusted;

    constructor() { super(); }

    public void update(uint input) {
        untrusted = input;     // OK: {sender} flows to {any}
        untrusted = trusted;   // OK: {this} flows to {any}
        trusted = input;       // Error: {sender} does not flow to {this}
    }
}
```

## Implicit flows

*Control flow* also carries information: when a branch depends on an untrusted condition, so does everything the branch does. The compiler tracks the integrity of the current control flow as the *program-counter label* `pc`, and an assignment requires both `pc` and the assigned value to flow to the label of the target:

```scif
contract Labels {
    uint{this} trusted;

    constructor() { super(); }

    public void update(uint input) {
        if (input > 0) {
            trusted = 1;   // Error: this branch depends on input, labeled {sender}
        }
    }
}
```

The assigned value `1` is a constant, but whether the `{this}` state variable `trusted` is assigned depends on `input`, which the contract does not trust.

A function's internal begin label sets `pc` at the start of its body. Functions and [Exceptions and Failures](/LanguageBasics/exceptions-and-failures) describe how calls, exceptions and failures affect `pc`.

## Endorsement

*Endorsement* explicitly raises the integrity of information. It is the only way for less trusted information to affect more trusted state, so every such decision is visible in the source code.

The expression `endorse(e, l_from -> l_to)` yields the value of `e` with its label raised from `l_from` to `l_to`:

```scif
trusted = endorse(input, sender -> this);
```

A *conditional endorsement* raises the label of a variable only in the branch where a check holds:

```scif
endorse(input, sender -> this) when (input <= 100) {
    trusted = input;   // OK: input is labeled {this} in this branch
} else {
    revert "input too large";
}
```

Conditional endorsement ties the trust decision to the check that justifies it, which makes it the recommended form. Several variables can be endorsed at once by listing them in brackets, as in `endorse([x, y], sender -> this) when (...)`.

A function can also endorse its control flow on entry, which Functions describes as auto-endorsement.

## Default labels

Unlabeled state variables and local variables are labeled `{this}`. A local variable in a function whose body runs with less integrity than the contract therefore needs an explicit label. The defaults for function labels, parameters, return values and exceptions are given in Functions.
