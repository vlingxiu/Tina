package com.moon.tree

/**
  * Created by Lin on 17/4/9.
  */
trait Tree {
  def setParent(t:Tree)

  def getParent():Tree

  def getPayload():Any

  def getChild(i:Int):Tree

  def getChildCount():Int

  def toStringTree():String

  def addChild(t:Tree)

  def deleteChild(i:Int):Tree

  def isNil():Boolean

  def setChild(i:Int,t:Tree)

  def setChildIndex(index:Int)
}
