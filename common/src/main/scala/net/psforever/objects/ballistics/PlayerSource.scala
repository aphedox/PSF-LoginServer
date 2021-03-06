// Copyright (c) 2017 PSForever
package net.psforever.objects.ballistics

import net.psforever.objects.Player
import net.psforever.objects.definition.ObjectDefinition
import net.psforever.types.{ExoSuitType, PlanetSideEmpire, Vector3}

final case class PlayerSource(name : String,
                              obj_def : ObjectDefinition,
                              faction : PlanetSideEmpire.Value,
                              exosuit : ExoSuitType.Value,
                              seated : Boolean,
                              health : Int,
                              armor : Int,
                              position : Vector3,
                              orientation : Vector3,
                              velocity : Option[Vector3] = None) extends SourceEntry {
  override def Name = name
  override def Faction = faction
  def Definition = obj_def
  def ExoSuit = exosuit
  def Seated = seated
  def Health = health
  def Armor = armor
  def Position = position
  def Orientation = orientation
  def Velocity = velocity
}

object PlayerSource {
  def apply(tplayer : Player) : PlayerSource = {
    PlayerSource(tplayer.Name, tplayer.Definition, tplayer.Faction, tplayer.ExoSuit, tplayer.VehicleSeated.nonEmpty,
      tplayer.Health, tplayer.Armor, tplayer.Position, tplayer.Orientation, tplayer.Velocity)
  }
}
