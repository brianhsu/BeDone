package org.bedone.snippet

import org.bedone.model._
import net.liftweb.util.Helpers._


class DashBoard
{
    val currentUser = CurrentUser.get.get

    private val actions = Action.findByUser(currentUser).openOr(Nil)
    private val totalActions = actions.size
    private val doneActions = actions.filter(_.action.isDone.is).size
    private val doneActionsPercent = totalActions match {
        case 0     => 0
        case total => (doneActions.toDouble / total) * 100
    }

    private val delegateds = Delegated.findByUser(currentUser).openOr(Nil)
    private val totalDelegated = delegateds.size
    private val doneDelegated = delegateds.filter(_.action.isDone.is).size
    private val doneDelegatedPercent = totalDelegated match {
        case 0     => 0
        case total => (doneDelegated.toDouble / total) * 100
    }

    private val scheduleds = Scheduled.findByUser(currentUser).openOr(Nil)
    private val totalScheduled = scheduleds.size
    private val doneScheduled = scheduleds.filter(_.action.isDone.is).size
    private val doneScheduledPercent = totalScheduled match {
        case 0     => 0
        case total => (doneScheduled.toDouble / total) * 100
    }

    def render = {
        "#nextActionProgress *" #> "%d / %d".format(doneActions, totalActions) &
        "#nextActionBar [style]" #> ("width: " + doneActionsPercent + "%") &
        "#delegatedProgress *" #> "%d / %d".format(doneDelegated, totalDelegated) &
        "#delegatedBar [style]" #> ("width: " + doneDelegatedPercent + "%") &
        "#scheduledProgress *" #> "%d / %d".format(totalScheduled, doneScheduled) &
        "#scheduledBar [style]" #> ("width: " + doneScheduledPercent + "%")

    }
}
