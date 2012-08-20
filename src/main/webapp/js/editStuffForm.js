function prepareInboxEditForm() 
{
    $( "#inboxTopic" ).autocomplete({
        source: "/autocomplete/topic",
        select: function(event, ui) { 
            $("#inboxTopic").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#inboxProject" ).autocomplete({
        source: "/autocomplete/project",
        select: function(event, ui) { 
            $("#inboxProject").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#inboxDeadline input" ).datepicker({
        dateFormat: "yy-mm-dd",
        onClose: function(dateText) {
            $("#inboxDeadline input").val(dateText)
            $("#inboxDeadline input").blur()
        }
    });

    $('#inboxSave').click(function () {
        $(this).button('loading')
    })
}

function prepareStuffEditForm() {
    $( "#inputContact" ).autocomplete({
        source: "/autocomplete/contact",
        select: function(event, ui) { 
            $("#inputContact").val(ui.item.label)
            $("#inputContact").blur()
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

    $( "#editTicklerDate input" ).datepicker({
        dateFormat: "yy-mm-dd",
        onClose: function(dateText) {
            $("#editTicklerDate input").val(dateText)
            $("#editTicklerDate input").blur()
        }
    });

    $( "#inputContext" ).autocomplete({
        source: "/autocomplete/context",
        select: function(event, ui) { 
            $("#inputContext").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

}
