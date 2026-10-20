# Repository development guide

Read this file before changing the project.

## Java naming

- Use descriptive camelCase names for parameters, local variables, fields, and lambda parameters.
- Do not use single-letter identifiers such as `c`, `p`, `r`, `s`, `t`, or `x`.
- Names must communicate the represented domain value, for example `client`, `project`, `request`, `tenantId`, `pageable`, and `compositionComponent`.
- A conventional index such as `index` must be written explicitly; do not use `i`.

## Formatting

- After creating or modifying source files, always run the IntelliJ formatter using `.idea/codeStyles/Project.xml`.
- Format all affected Java production and test classes before running verification.
- Treat formatting as a required implementation step, not an optional cleanup.

## Verification

- After formatting, run `MAVEN_USER_HOME=/tmp/bdsystems-m2 ./mvnw -q verify`.
