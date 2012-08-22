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

function prepareActionEditForm()
{
    $( "#actionContext" ).autocomplete({
        source: "/autocomplete/context",
        select: function(event, ui) { 
            $("#actionContext").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#actionTopic" ).autocomplete({
        source: "/autocomplete/topic",
        select: function(event, ui) { 
            $("#actionTopic").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#actionProject" ).autocomplete({
        source: "/autocomplete/project",
        select: function(event, ui) { 
            $("#actionProject").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#actionDeadline input" ).datepicker({
        dateFormat: "yy-mm-dd",
        onClose: function(dateText) {
            $("#actionDeadline input").val(dateText)
            $("#actionDeadline input").blur()
        }
    });

    $('#actionSave').click(function () {
        $(this).button('loading')
    })

}

function prepareDelegateEditForm()
{
    $( "#delegateTopic" ).autocomplete({
        source: "/autocomplete/topic",
        select: function(event, ui) { 
            $("#delegateTopic").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#delegateProject" ).autocomplete({
        source: "/autocomplete/project",
        select: function(event, ui) { 
            $("#delegateProject").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#delegateContact input" ).autocomplete({
        source: "/autocomplete/contact",
        select: function(event, ui) { 
            $("#delegateContact input").val(ui.item.label)
            $("#delegateContact input").blur()
        }
    });

    $( "#delegateDeadline input" ).datepicker({
        dateFormat: "yy-mm-dd",
        onClose: function(dateText) {
            $("#delegateDeadline input").val(dateText)
            $("#delegateDeadline input").blur()
        }
    });

    $('#delegateSave').click(function () {
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

function prepareMaybeEditForm()
{
    $( "#maybeTopic" ).autocomplete({
        source: "/autocomplete/topic",
        select: function(event, ui) { 
            $("#maybeTopic").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#maybeProject" ).autocomplete({
        source: "/autocomplete/project",
        select: function(event, ui) { 
            $("#maybeProject").val(ui.item.label)
            $(this).closest("form").submit()
        }
    });

    $( "#maybeTicklerDate input" ).datepicker({
        dateFormat: "yy-mm-dd",
        onClose: function(dateText) {
            $("#maybeTicklerDate input").val(dateText)
            $("#maybeTicklerDate input").blur()
        }
    });

}

