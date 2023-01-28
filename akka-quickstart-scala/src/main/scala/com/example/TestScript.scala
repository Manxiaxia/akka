package com.example

object TestScript extends App {
  val withReturn = isPositiveNum(-1)
  val withoutReturn = isPositiveNumNoReturn(-1)
  println(withReturn)
  println(withoutReturn)

  def isPositiveNum(num : Int) : Boolean = {
    val returnTrue = return true
    val returnFalse = return false

    if(num  > 0) returnTrue else returnFalse
  }

  def isPositiveNumNoReturn(num : Int) : Boolean = {
    if(num > 0) true else false
  }
}

