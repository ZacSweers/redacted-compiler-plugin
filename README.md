redacted-compiler-plugin
========================

A Kotlin compiler plugin that generates redacted `toString()` implementations.

Inspired by the [`auto-value-redacted`](https://github.com/square/auto-value-redacted) extension for AutoValue.

## Usage

Include the gradle plugin in your project, define a `@Redacted` annotation, and apply it to any 
properties that you wish to redact.

```kotlin
@Retention(BINARY)
@Target(PROPERTY)
annotation class Redacted

data class User(val name: String, @Redacted val phoneNumber: String)
```

When you call `toString()` any `@Redacted` properties are hidden:

```
User(name=Bob, phoneNumber=██)
```

If your annotation is applied to the class, then `toString()` will emit a single replacement string:

```kotlin
@Retention(BINARY)
@Target(CLASS)
annotation class Redacted

@Redacted
data class SensitiveData(val ssn: String, val birthday: String)
```

```
SensitiveData(██)
```

## Installation

Apply the gradle plugin and define values on the `redacted` extension.

```gradle
buildscript {
  dependencies {
    classpath "dev.zacsweers.redacted:redacted-compiler-plugin-gradle:x.y.z"
  }  
}

apply plugin: 'dev.zacsweers.redacted.redacted-gradle-plugin'

redacted {
  redactedAnnotation = "your.annotation.here.Redacted" // Required
  enabled = true // Default
  replacementString = "██" // Default
}

```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snapshots].

## Caveats

Kotlin compiler plugins are not a stable API! Compiled outputs from this plugin _should_ be stable,
but usage in newer versions of kotlinc are not guaranteed to be stable.

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

 [snapshots]: https://oss.sonatype.org/content/repositories/snapshots/