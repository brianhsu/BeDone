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

    $( "#inputContact" ).autocomplete({
        source: "/autocomplete/contact",
        select: function(event, ui) { 
            $("#inputContact").val(ui.item.label)
            $("#inputContact").blur()
        }
    });

    $( "#editStuffDeadline input" ).datepicker({
        dateFormat: "yy-mm-dd",
        onClose: function(dateText) {
            $("#editStuffDeadline input").val(dateText)
            $("#editStuffDeadline input").blur()
        }
    });

    $( "#editStartTime input" ).datetimepicker({
        dateFormat: "yy-mm-dd",
        timeFormat: 'hh:mm',
        onClose: function(dateText) {
            $("#editStartTime input").val(dateText)
            $("#editStartTime input").blur()
        }
    });

    $( "#editEndTime input" ).datetimepicker({
        dateFormat: "yy-mm-dd",
        timeFormat: 'hh:mm',
        onClose: function(dateText) {
            $("#editEndTime input").val(dateText)
            $("#editEndTime input").blur()
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
