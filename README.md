redacted-compiler-plugin
========================

A multiplatform Kotlin compiler plugin that generates redacted `toString()` implementations.

Inspired by the [`auto-value-redacted`](https://github.com/square/auto-value-redacted) extension for AutoValue.

## Usage

Include the gradle plugin in your project, define a `@Redacted` annotation, and apply it to any
properties that you wish to redact.

```kotlin
@Retention(SOURCE)
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
@Retention(SOURCE)
@Target(CLASS)
annotation class Redacted

@Redacted
data class SensitiveData(val ssn: String, val birthday: String)
```

```
SensitiveData(██)
```

## Installation

Apply the gradle plugin.

```gradle
plugins {
  id("dev.zacsweers.redacted") version <version>
}
```

And that's it! The default configuration will add the multiplatform `-annotations` artifact (which has a
`@Redacted` annotation you can use) and wire it all automatically. Just annotate what you want to
redact.

You can configure custom behavior with properties on the `redacted` extension.

```kotlin
redacted {
  // Define custom annotations. The -annotations artifact won't be automatically added to
  // dependencies if you define your own!
  // Note that these must be in the format of a string where packages are delimited by '/' and
  // classes by '.', e.g. "kotlin/Map.Entry"
  redactedAnnotations.add("dev/zacsweers/redacted/annotations/Redacted") // Default

  // Define custom unredacted annotations.
  unredactedAnnotations.add("dev/zacsweers/redacted/annotations/Unredacted") // Default

  // Define whether this plugin is enabled on this compilation. Useful if you want to
  // gate this behind a dynamic build configuration.
  enabled = true // Default

  // Define a custom replacement string for redactions.
  replacementString = "██" // Default
}
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snapshots].

## Supported platforms

The compiler plugin itself supports all multiplatform project types. The first-party annotations artifact is also
multiplatform and supports all common JVM, JS, and native targets.

## IDE Support

FIR diagnostics can render in the IDE under the following conditions:

1. The K2 Kotlin IDE plugin must be enabled.
2. The IntelliJ `kotlin.k2.only.bundled.compiler.plugins.enabled` registry key must be set to `false`.

## Caveats

- Kotlin compiler plugins are not a stable API! Compiled outputs from this plugin _should_ be stable,
but usage in newer versions of kotlinc are not guaranteed to be stable.


## Advanced Usage

In situations where it is desirable to redact everything and opt-out on certain properties,
two options are provided:

**Class redaction**

For one-off classes that may contain a large number of fields that should be redacted, you can augment the `@Redacted`
class behavior:

```kotlin
@Redacted
data class User(@Unredacted val name: String, val phoneNumber: String, val ssn: String)
```

```
User(name=Bob, phoneNumber=██, ssn=██)
```

**Supertype redaction**

For situations where you need to enforce that an API only accepts redacted inputs, you can apply `@Redacted` to a
parent interface.

```kotlin
@Redacted
interface RedactedObject

data class User(@Unredacted val name: String, val phoneNumber: String, val ssn: String) : RedactedObject
```

```
User(name=Bob, phoneNumber=██, ssn=██)
```

License
-------

    Copyright (C) 2018 Zac Sweers

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 [snapshots]: https://oss.sonatype.org/content/repositories/snapshots/
