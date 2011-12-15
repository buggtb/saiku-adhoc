/**
 * Dialog for column configuration
 */
var ExportFileModal = Modal.extend({
    type: "export",
    
    buttons: [
        { text: "Save", method: "save" },
        { text: "Cancel", method: "close" }
    ],
    
    events: {
        'click a': 'call' 
    },
    
    initialize: function(args) {

        // Initialize properties
        _.extend(this, args);
        this.options.title = "Export " + args.extension;
        this.message = "Fetching repository...";
        this.query = args.workspace.query;
        _.bindAll(this, "finished","post_render");
              
        // Resize when rendered
        this.bind('open', this.post_render);

		this.render(); 
  
  		//filetree bekommt noch die information, welcher typ...
  		this.filetree = new FileTree({
            dir: "bi-developers",
            extensions: "cda"
        });
  		this.filetree.fetch({ success: this.post_render });

    },

    post_render: function(args) {
		
		this.treeTemplate = this.filetree.get('template');

    	$(this.el).find('.dialog_body').html(this.treeTemplate);
    	
    	$(this.el).find('.filetree').treeview({
    		collapsed: true,
   			toggle: function() {
   				alert(this + " has been toggled");
   				}
    	});
    	
    	//$("span.file, span.folder").click(function() { alert($(this).text()); });

        $(this.el).parents('.ui-dialog').css({ width: "200px" });
        
    },
    
    save: function() {
        return false;
    },
    
    finished: function() {
        $(this.el).dialog('destroy').remove();
    }
});