redacted-compiler-plugin
========================

A proof-of-concept Kotlin compiler plugin that generates redacted `toString()` implementations.

Similar to [`auto-value-redacted`](https://github.com/square/auto-value-redacted), where the following class:

```kotlin
data class Person(val name: String, @Redacted val ssn: String)
```

produces the following `toString()`

```kotlin
val person = Person("John Doe", "123-456-7890")

println(person) // --> Person(name=Foo, ssn="██")
```

Running
----------
Run the `Runner` main method in the `sample`.

Caveats
----------
* The kotlin or gradle deamons do caching I don't quite understand, so to re-run you may need to run `./gradlew --stop` and `./gradlew clean` first between runs.

License
-------

    Copyright (C) 2018 Zac Sweers

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
