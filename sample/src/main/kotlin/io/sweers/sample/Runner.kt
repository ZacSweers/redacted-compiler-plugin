package io.sweers.sample

class Runner {

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val person = Person("John Doe", "123-456-7890")
      println(person.toString())
    }
  }
}
