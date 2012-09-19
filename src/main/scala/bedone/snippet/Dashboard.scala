package org.bedone.snippet

import net.liftweb.util.Helpers._

class DashBoard
{
    def render = {
        import net.liftweb.http.js.JE.Str
        import net.liftweb.http.js.JsCmd
        import net.liftweb.http.js.JsCmds.Alert
  
        import net.liftmodules.combobox.ComboItem
        import net.liftmodules.combobox.ComboBox

        val options = ("placeholder" -> Str("Choice the flavor your like")) :: Nil

        // This is where you build your combox suggestion
        def onSearching(term: String): List[ComboItem] = {
            val flavor = ComboItem("f1", "Valina") :: ComboItem("f2", "Fruit") ::
                         ComboItem("f3", "Banana") :: ComboItem("f4", "Apple") ::
                         ComboItem("f5", "Chocolate") :: Nil
    
            flavor.filter(_.text.contains(term))
        }

        // What you want to do when user selected or cancel an item.
        //
        // If user cancel selection by the X button in the combbox, 
        // selected will be None, otherwise it will be Some[ComboItem].
        def onItemSelected(selected: Option[ComboItem]): JsCmd = {
            println("selected:" + selected)

            // The returned JsCmd will be executed on client side.
            Alert("You selected:" + selected)
        }

        // What you want to do if user added an item that
        // does not exist when allowCreate = true.
        def onItemAdded(text: String): JsCmd = {
            // save this item to database or anything you want to do
            println("user added " + text)

            // The returned JsCmd will be executed on client side.
            Alert("Saved " + text + " to database")
        }

        val comboBox = ComboBox(None, onSearching _, onItemSelected _, onItemAdded _, options)

        "name=contactInput" #> comboBox.comboBox
    }
}
