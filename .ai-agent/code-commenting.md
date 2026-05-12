### Code Commenting

Add (or improve) JavaDoc comments for the provided code WITHOUT changing runtime behavior.

#### Goal

Generate JavaDoc that is accurate, specific, and useful for developers. The JavaDoc must follow standard JavaDoc conventions and be suitable for publishing as official documentation.

#### Scope

- Add a CLASS-LEVEL JavaDoc for every class/interface/enum.
- Add METHOD-LEVEL JavaDoc for every public/protected method (and private methods only if they are complex or non-obvious).
- Add FIELD-LEVEL JavaDoc for public/protected constants and any injected dependencies where the purpose is not obvious.
- Add JavaDoc for constructors if they do more than simple assignment or have important constraints.

#### Hard Rules

1) Do NOT modify logic, signatures, annotations, imports, formatting, or code structure. Only add/edit comments.
2) Do NOT invent behavior. If something is unclear, infer only from the code and name-based conventions. If still uncertain, document cautiously (“Represents…”, “Handles…”, “Attempts to…”).
3) Do NOT write generic filler like “This method does X” unless it adds real detail.
4) Reflect Spring Boot idioms correctly: controllers, services, repositories, configuration, components, filters, interceptors, schedulers, event listeners, etc.
5) Keep JavaDoc concise but descriptive. Prefer clarity over verbosity.
6) Ensure JavaDoc compiles cleanly (no malformed tags, no broken @link usage).
7) Do not write single line comments before or beside code lines or code blocks. Comments should strictly be multi-lines and are only acceptable on top of the class at class level and on top of methods at method level.

#### Java Doc Style Requirements

- Start with a one-line summary sentence in present tense, ending with a period.
- Follow with short paragraphs explaining purpose, responsibilities, and key behavior.
- Use proper tags where applicable:
  - @param for each parameter (explain constraints, nullability expectations, units, format)
  - @return describe what is returned, including when null/empty is possible
  - @throws only for meaningful/expected exceptions (and when they occur)
  - @apiNote for important usage notes
  - @implNote for implementation details that matter
  - @implSpec for interface method specifications (when relevant)
  - @see and {@link ...} for related classes/components
- Use HTML lists (<ul><li>...</li></ul>) only when it improves readability.
- Mention relevant Spring annotations in class docs when helpful (e.g., @Service, @RestController) but do not describe obvious annotation behavior unless it affects usage.

#### Spring Boot Specific Guidance

For each kind of class, include these details when applicable:
- Controller: endpoint purpose, auth/validation expectations, response semantics, status codes, error handling approach.
- Service: business responsibility, transactional boundaries, idempotency, side effects.
- Repository/DAO: persistence responsibility, query intent, paging/sorting behavior.
- Configuration: what beans are created and why, order/conditional loading, properties used.
- DTO: what it represents, field meanings, serialization notes, validation constraints.
- Entity: domain meaning, key fields, relationships, constraints, lifecycle notes.
- Scheduled/Async: schedule cadence, concurrency assumptions, failure/retry behavior.

### Native Query Specific Guidance

For each native query that is generated or is already there in the codebase, the following policies are applicable during generation of code comments:
- Native queries can have their own multi-line code comments associated with it. This is the only time that the codebase can have code comments other than just classes and methods.
- The native query must be explained with absolute details at great length involving what exactly the SQL query is trying to do.
- Native query includes both raw SQL queries and JPQL.

#### Nullability & Contracts

- If you can infer nullability, state it in @param/@return text (e.g., “must not be null”, “may be null if …”).
- If annotations exist (e.g., @NotNull/@Nullable), align documentation with them.

#### Security & Performance

- If code involves authentication/authorization, explicitly document security assumptions.
- If code is performance-sensitive (batch ops, caching, paging, streaming), document it briefly.

#### Output Format

Return ONLY the updated code with added JavaDoc comments. Do not add explanations outside the code.
Preserve existing comments, but upgrade them if they are incorrect or too vague.
