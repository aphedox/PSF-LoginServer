// Copyright (c) 2017 PSForever
package net.psforever.objects.vital.resolution

import net.psforever.objects.{Player, TurretDeployable, Vehicle}
import net.psforever.objects.ballistics.{PlayerSource, ResolvedProjectile}
import net.psforever.objects.ce.{ComplexDeployable, SimpleDeployable}
import net.psforever.objects.vital.projectile.ProjectileCalculations

/**
  * The base for the combining step of all projectile-induced damage calculation function literals.
  */
trait ResolutionCalculations {
  /**
    * The exposed entry for the calculation function literal defined by this base.
    * @param damages the function literal that accumulates and calculates damages
    * @param resistances the function literal that collects resistance values
    * @param data the historical `ResolvedProjectile` information
    * @return a function literal that encapsulates delayed modification instructions for certain objects
    */
  def Calculate(damages : ProjectileCalculations.Form, resistances : ProjectileCalculations.Form, data : ResolvedProjectile) : ResolutionCalculations.Output
}

object ResolutionCalculations {
  type Output = (Any)=>Unit
  type Form = (ProjectileCalculations.Form, ProjectileCalculations.Form, ResolvedProjectile)=>Output

  def NoDamage(data : ResolvedProjectile)(a : Int, b : Int) : Int = 0

  def InfantryDamageAfterResist(data : ResolvedProjectile) : (Int, Int)=>(Int, Int) = {
    data.target match {
      case target : PlayerSource =>
        InfantryDamageAfterResist(target.health, target.armor)
      case _ =>
        InfantryDamageAfterResist(0, 0)
    }
  }

  def InfantryDamageAfterResist(currentHP : Int, currentArmor : Int)(damages : Int, resistance : Int) : (Int, Int) = {
    if(damages > 0 && currentHP > 0) {
      if(currentArmor <= 0) {
        (damages, 0) //no armor; health damage
      }
      else if(damages > resistance) {
        val resistedDam = damages - resistance
        //(resistedDam, resistance)
        if(resistance <= currentArmor) {
          (resistedDam, resistance) //armor and health damage
        }
        else {
          (resistedDam + (resistance - currentArmor), currentArmor) //deplete armor; health damage + bonus
        }
      }
      else {
        (0, damages) //too weak; armor damage (less than resistance)
      }
    }
    else {
      (0, 0) //no damage
    }
  }

  def MaxDamageAfterResist(data : ResolvedProjectile) : (Int, Int)=>(Int, Int) = {
    data.target match {
      case target : PlayerSource =>
        MaxDamageAfterResist(target.health, target.armor)
      case _ =>
        MaxDamageAfterResist(0, 0)
    }
  }

  def MaxDamageAfterResist(currentHP : Int, currentArmor : Int)(damages : Int, resistance : Int) : (Int, Int) = {
    val resistedDam = damages - resistance
    if(resistedDam > 0 && currentHP > 0) {
      if(currentArmor <= 0) {
        (resistedDam, 0) //no armor; health damage
      }
      else if(resistedDam >= currentArmor) {
        (resistedDam - currentArmor, currentArmor) //deplete armor; health damage
      }
      else {
        (0, resistedDam) //too weak; armor damage (less than resistance)
      }
    }
    else {
      (0, 0) //no damage
    }
  }

  /**
    * Unlike with `Infantry*` and with `Max*`'s,
    * `VehicleDamageAfterResist` does not necessarily need to validate its target object.
    * The required input is sufficient.
    * @param data the historical `ResolvedProjectile` information
    * @return a function literal for dealing with damage values and resistance values together
    */
  def VehicleDamageAfterResist(data : ResolvedProjectile) : (Int, Int)=>Int = {
    VehicleDamageAfterResist
  }

  def VehicleDamageAfterResist(damages : Int, resistance : Int) : Int = {
    if(damages > resistance) {
      damages - resistance
    }
    else {
      damages
    }
  }

  def NoApplication(damageValue : Int, data : ResolvedProjectile)(target : Any) : Unit = { }

  /**
    * The expanded `(Any)=>Unit` function for infantry.
    * Apply the damage values to the health field and personal armor field for an infantry target.
    * @param damageValues a tuple containing damage values for: health, personal armor
    * @param data the historical `ResolvedProjectile` information
    * @param target the `Player` object to be affected by these damage values (at some point)
    */
  def InfantryApplication(damageValues : (Int, Int), data : ResolvedProjectile)(target : Any) : Unit = target match {
    case player : Player =>
      val (a, b) = damageValues
      //TODO Personal Shield implant test should go here and modify the values a and b
      if(player.isAlive && !(a == 0 && b == 0)) {
        player.History(data)
        if(player.Armor - b < 0) {
          player.Health = player.Health - a - (b - player.Armor)
          player.Armor = 0
        }
        else {
          player.Armor = player.Armor - b
          player.Health = player.Health - a
        }
      }
    case _ =>
  }

  /**
    * The expanded `(Any)=>Unit` function for vehicles.
    * Apply the damage value to the shield field and then the health field (that order) for a vehicle target.
    * @param damage the raw damage
    * @param data the historical `ResolvedProjectile` information
    * @param target the `Vehicle` object to be affected by these damage values (at some point)
    */
  def VehicleApplication(damage : Int, data : ResolvedProjectile)(target : Any) : Unit = target match {
    case vehicle : Vehicle =>
      if(vehicle.Health > 0) {
        vehicle.History(data)
        val shields = vehicle.Shields
        if(shields > damage) {
          vehicle.Shields = shields - damage
        }
        else if(shields > 0) {
          vehicle.Health = vehicle.Health - (damage - shields)
          vehicle.Shields = 0
        }
        else {
          vehicle.Health = vehicle.Health - damage
        }
      }
    case _ => ;
  }

  def SimpleDeployableApplication(damage : Int, data : ResolvedProjectile)(target : Any) : Unit = target match {
    case ce : SimpleDeployable =>
      if(ce.Health > 0) {
        ce.Health -= damage
        ce.History(data)
      }
    case _ =>
  }

  def ComplexDeployableApplication(damage : Int, data : ResolvedProjectile)(target : Any) : Unit = target match {
    case ce : ComplexDeployable =>
      if(ce.Shields > 0) {
        if(damage > ce.Shields) {
          ce.Health -= (damage - ce.Shields)
          ce.Shields = 0
        }
        else {
          ce.Shields -= damage
        }
        ce.History(data)
      }
      else if(ce.Health > 0) {
        ce.Health -= damage
        ce.History(data)
      }

    case ce : TurretDeployable =>
      if(ce.Shields > 0) {
        if(damage > ce.Shields) {
          ce.Health -= (damage - ce.Shields)
          ce.Shields = 0
        }
        else {
          ce.Shields -= damage
        }
        ce.History(data)
      }
      else if(ce.Health > 0) {
        ce.Health -= damage
        ce.History(data)
      }

    case _ => ;
  }
}
