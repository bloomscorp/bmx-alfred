## Code Formatting

1. All tabs are 4 spaces wide.
2. All lines should be properly indented.
3. All class names are in PascalCase.
4. All method names are in camelCase.
5. All variable names are in camelCase.
6. All constant names are in UPPERCASE_SNAKE_CASE.
7. If a package name is two words, the package name should be in snake_case.
8. ORMs usually should not be plural.
9. All methods must have `private` access modifier by default, unless used by any external class or package. Methods should be `protected` if they is being used by classes within the same package. It may be `public` otherwise.
10. All class variables must have `private` access modifier by default, unless used by any external class or package. Class variables should be `protected` if they is being used by classes within the same package. It may be `public` otherwise.
11. All private class variables should be the first thing in a class.
12. All protected class variables should be after private class variables.
13. All public class variables should be after protected class variables.
14. The constructor should be the next thing after class variables.
15. All private methods should be after the constructor.
16. All protected methods should be after private methods.
17. All public methods should be after protected methods.
18. All classes and methods should be documented with Javadoc.
19. All classes and methods should use the keywords "this" and "super" properly and appropriately.
20. [IMPORTANT] Do not keep any "System.out.println" calls or any code that writes to the CLI as part of production code. It can only be used for testing. However, even during testing, using appropriate debugger is recommended and not "print testing / debugging".
21. While using"if" statments, if the conditional block is only 1 line long, do not use curly braces. However, if the conditional block is more than 1 line long, use curly braces.
22. No secrets should be hardcoded in the code. All secrets should be stored in the properties files, strictly.