package org.bedone.snippet

import net.liftweb.json._
import net.liftweb.json.JsonParser

import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._

import net.liftweb.http.SHtml

import net.liftweb.util.Helpers

import scala.xml.NodeSeq

case class ComboItem(id: String, text: String)

class ComboBox(ajaxURL: String, allowCreate: Boolean, 
               jsonOptions: List[(String, String)] = Nil)
{
    private implicit val formats = DefaultFormats
    private val NewItemPrefix = Helpers.nextFuncName

    val comboBoxID = Helpers.nextFuncName

    def onItemSelected(id: String, text: String): JsCmd = { Noop }
    def onItemAdded(text: String): JsCmd = { Noop }

    private def onItemSelected_*(value: String): JsCmd = {

        val item = JsonParser.parse(value).extract[ComboItem]

        item.id match {
            case x if x.startsWith(NewItemPrefix) => onItemAdded(item.text)
            case _ => onItemSelected(item.id, item.text)
        }
    }

    def comboBox: NodeSeq = {
        
        val options = ("width" -> "'200px'") :: jsonOptions
        val jsOptions = JsRaw(options.map(t => "%s: %s" format(t._1, t._2)).mkString(","))

        val onSelectJS = {
            val jsExp = JsRaw("""getValue()""")
            SHtml.ajaxCall(jsExp, onItemSelected_* _)._2.toJsCmd
        }

        val ajaxJS = """{
            url: "%s",
            dataType: 'json',
            data: function (term, page) {
                return { 'term': term }
            },
            results: function (data, page) {
                return {results: data};
            }
        }""".format(ajaxURL)

        val onChangeJS = """
            $("#%s").on("change", function(e) { 
                %s 
            });
        """.format(comboBoxID, onSelectJS)

        val getValueJS = """
            function getValue() {
                var data = $('#%s').select2("data");
                console.log("QQ.id:" + data.id);
                console.log("QQ.text:" + data.text);
                return '{"id": "' + data.id + '", "text": "' + data.text + '" }';
            }
        """.format(comboBoxID)

        val createSearchChoiceJS = """
            function createNewItem (term, data) { 
                if ($(data).filter(function() { return this.text.localeCompare(term)===0; })
                           .length===0) {
                    console.log("add new:" + term);
                    return {"id": "%s" + term, "text": term};
                } 
            }
        """.format(NewItemPrefix)

        val select2JS = allowCreate match {
            case false => 
                """
                    $("#%s").select2({
                        %s,
                        ajax: %s
                    });
                """.format(comboBoxID, jsOptions.toJsCmd, ajaxJS)

            case true =>

                """
                     $("#%s").select2({
                        %s,
                        ajax: %s,
                        createSearchChoice: createNewItem
                    });
                """.format(comboBoxID, jsOptions.toJsCmd, ajaxJS)
        }

        val onLoad = OnLoad(
            JsRaw(
                onChangeJS ++
                getValueJS ++
                createSearchChoiceJS ++
                select2JS
            )
        )

        <head>
            {Script(onLoad)}
        </head>
        <input type="hidden" id={comboBoxID} />
    }
}
