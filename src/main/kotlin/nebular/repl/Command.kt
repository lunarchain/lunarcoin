package nebular.repl

class Command(val command: String) {
  val name = getCommandName(command)

  private fun getCommandName(command: String): String {
    val leftPar = command.indexOf("(")
    if (leftPar == -1) {
      return command
    } else {
      return command.substring(0, leftPar)
    }
  }

  val args: List<String>
    get() {
      val leftPar = command.indexOf('(')
      val rightPar = command.lastIndexOf(')')
      if (leftPar != -1 && rightPar != -1) {
        val argStr = command.substring(leftPar + 1, rightPar)
        if (argStr.trim().length == 0) {
          return emptyList()
        } else {
          val args = argStr.split(",").map { it.trim() }
          return args
        }
      }

      return emptyList()
    }

  fun getStringArgument(index: Int): String {
    if (index < args.size) {
      return args[index].removeSurrounding("\"")
    } else {
      throw IndexOutOfBoundsException()
    }
  }

  fun getIntArgument(index: Int): Int {
    if (index < args.size) {
      return args[index].toInt()
    } else {
      throw IndexOutOfBoundsException()
    }
  }

}