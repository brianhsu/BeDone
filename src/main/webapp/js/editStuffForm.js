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

function prepareReferenceEditForm() 
{
    $( "#referenceTopic" ).autocomplete({
        source: "/autocomplete/topic",
        select: function(event, ui) { 
            $("#referenceTopic").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#referenceProject" ).autocomplete({
        source: "/autocomplete/project",
        select: function(event, ui) { 
            $("#referenceProject").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $('#referenceSave').click(function () {
        $(this).button('loading')
    })
}

function prepareScheduledEditForm() 
{
    $( "#scheduledTopic" ).autocomplete({
        source: "/autocomplete/topic",
        select: function(event, ui) { 
            $("#scheduledTopic").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#scheduledProject" ).autocomplete({
        source: "/autocomplete/project",
        select: function(event, ui) { 
            $("#scheduledProject").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#scheduledStartTime input" ).datetimepicker({
        dateFormat: "yy-mm-dd",
        timeFormat: 'hh:mm',
        onClose: function(dateText) {
            $("#scheduledStartTime input").val(dateText)
            $("#scheduledStartTime input").blur()
        }
    });

    $( "#scheduledEndTime input" ).datetimepicker({
        dateFormat: "yy-mm-dd",
        timeFormat: 'hh:mm',
        onClose: function(dateText) {
            $("#scheduledEndTime input").val(dateText)
            $("#scheduledEndTime input").blur()
        }
    });
}
