package org.bedone.snippet

import org.bedone.model._
import net.liftweb.util.Helpers._
import net.liftweb.http.S

class NextActionModal
{
    println("stuffID:" + S.attr("stuffID"))

    lazy val stuff = S.attr("stuffID")
                      .flatMap(stuffID => Stuff.findByID(stuffID.toInt))
                      .openOrThrowException("No such Stuff")

    def render = {
        "#stuffID" #> stuff.idField.is
    }
}
