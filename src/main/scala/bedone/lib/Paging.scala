package org.bedone.lib

import org.bedone.model._

import net.liftweb.common._
import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd

import scala.xml.NodeSeq

class Paging[T](val data: Array[T], pageLength: Int, pageGroup: Int, 
                onSwitchPage: (Paging[T], Int) => JsCmd) 
{
    val size = data.size
    val totalPage = (size % pageLength) match {
        case 0 => size / pageLength
        case _ => (size / pageLength) + 1
    }

    def apply(page: Int) = {
        val offset = (page - 1) * pageLength
        data.slice(offset, offset + pageLength).toList
    }

    def pageSelector(currentPage: Int): NodeSeq = {

        if (currentPage <= 0 || currentPage > totalPage) {
            return NodeSeq.Empty
        }

        val startPage = (currentPage % pageGroup) match {
            case 0 => ((currentPage / pageGroup) - 1) * pageGroup + 1
            case x => (currentPage / pageGroup) * pageGroup +1
        }
        val endPage = (startPage + pageGroup - 1) match {
            case x if x > totalPage => totalPage
            case x => x
        }

        val pageList = (startPage to endPage).map { page => 
            val className = if (page == currentPage) "active" else ""
            val cssBinding = {
                "a [onclick]" #> SHtml.onEvent(s => onSwitchPage(this, page)) &
                "a *" #> page
            }

            cssBinding(<li class={className}><a href="javascript: void(0)">1</a></li>)
        }

        val prevPage = if (currentPage - 1 >= 1) currentPage - 1 else currentPage
        val nextPage = if (currentPage + 1 <= totalPage) currentPage + 1 else currentPage
        val disableNext = if (currentPage == totalPage) "disabled" else ""
        val disablePrev = if (currentPage == 1) "disabled" else ""
        val cssBinding = {
            "#prevContactPage [onclick]" #> SHtml.onEvent(s => onSwitchPage(this, prevPage)) &
            "#nextContactPage [onclick]" #> SHtml.onEvent(s => onSwitchPage(this, nextPage))
        }

        cssBinding(
            <ul>
               <li class={disablePrev} id="prevContactPage"><a href="javascript: void:(0)">&laquo;</a></li>
              {pageList}
               <li class={disableNext} id="nextContactPage"><a href="javascript: void:(0)">&raquo;</a></li>
            </ul>
        )
    }
}
