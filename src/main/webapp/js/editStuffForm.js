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

function prepareStuffEditForm() {

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


}
