function prepareStuffEditForm() {

    $( "#inputTopic" ).autocomplete({
        source: "/autocomplete/topic",
        select: function(event, ui) { 
            $("#inputTopic").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#inputProject" ).autocomplete({
        source: "/autocomplete/project",
        select: function(event, ui) { 
            $("#inputProject").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#editStuffDeadline input" ).datepicker({
        dateFormat: "yy-mm-dd",
        onClose: function(dateText) {
            $("#editStuffDeadline input").val(dateText)
            $("#editStuffDeadline input").blur()
        }
    });

    $( "#inputContext" ).autocomplete({
        source: "/autocomplete/context",
        select: function(event, ui) { 
            $("#inputContext").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $('#editStuffSave').click(function () {
        $(this).button('loading')
    })
}
