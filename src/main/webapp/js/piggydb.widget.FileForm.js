(function(module) {
	
	var _ID = "file-form";
	
	var _open = function(args, fragment, modal, onSaved) {
		jQuery("#" + _ID).remove();
		
		if (fragment != null) args.id = fragment.id();
		
		piggydb.util.blockPageDuringAjaxRequest();
		jQuery.get("partial/file-form.htm", args, function(html) {
			if (!module.FragmentFormBase.checkOpenError(html)) {
				jQuery("body").append(html);
				var form = new _class(jQuery("#" + _ID));
				form.fragment = fragment;
				form.modal = modal;
				form.onSaved = onSaved;
				form.open();
			}
		});
	};
	
	var _initialHeight = 150;
	
	var _class = function(element) {
		module.FragmentFormBase.call(this, element);
		
		window.fileForm = this;
		
		this.id = _ID;
		this.modal = false;
		this.indicator = this.element.find("span.indicator");
		this.fragment = null;		// target fragment widget to be updated
		this.onSaved = null;
		
		this.prepareCommonInputs();
	};
	
	_class.openToAdd = function() {
		_open({}, null, false, function(newId) {
			piggydb.widget.FragmentsView.refreshViews(newId);
			piggydb.widget.TagPalette.refreshAll();
		});
	};
	
	_class.openToUpdate = function(button) {
		_open({}, new piggydb.widget.Fragment(button), false, function(id) {
			piggydb.widget.TagPalette.refreshAll();
		});
	};
	
	_class.openToEmbed = function(onSaved) {
		_open({}, null, true, onSaved);
	};
	
	_class.openToAddChild = function(parentId) {
		_open({parentId: parentId}, null, false, function(newId) {
			piggydb.widget.Fragment.reloadRootChildNodes(parentId, newId);
			piggydb.widget.TagPalette.refreshAll();
		});
	};
	
	_class.openToCreateWithTag = function(tagId) {
		_open({tagId: tagId}, null, false, function(newId) {
			piggydb.widget.FragmentsView.refreshViews(newId);
			piggydb.widget.TagPalette.refreshAll();
		});
	};
	
	_class.openToCreateWithFilter = function(filterId) {
		_open({filterId: filterId}, null, false, function(newId) {
			piggydb.widget.FragmentsView.refreshViews(newId);
			piggydb.widget.TagPalette.refreshAll();
		});
	};
	
	_class.prototype = jQuery.extend({
		
		open: function() {
			var outer = this;
			
			this.element.dialog({
				dialogClass: "dialog-file-form",
				resizable: false,
				width: 600,
				height: _initialHeight,
				modal: outer.modal,
				closeOnEscape: false,
				close: function(event, ui) {
					piggydb.widget.imageViewer.close();
					window.fileForm = null;
				}
			});
		
			if (this.fragment == null) 
				this.buttonsDiv().hide();
			
			this.element.find("input.file").change(function() {
				outer.setDialogHeight(_initialHeight + 15);
				outer.previewDiv().empty().putLoadingIcon("margin: 5px 10px;");
				outer.element.find("form").submit();
			});
			this.element.find("button.register").click(function() {
				outer.clearErrors();
				outer.block();
				
				var values = outer.element.find("form").serializeArray();
				jQuery.post("partial/save-file.htm", values, function(html) {
					if (outer.checkErrors(html)) {
						piggydb.widget.imageViewer.close();
						outer.unblock();
					}
					else {
						outer.processResponseOnSaved(html, outer.fragment);
						outer.close();
					}
				});
			});
			this.element.find("div.preview img").load(function() {
				outer.adjustHeight();
			});
		},
		
		setDialogHeight: function(height) {
			this.element.dialog("option", "height", height);
		},
		
		buttonsDiv: function() {
			return this.element.find("div.buttons");
		},
		
		previewDiv: function() {
			return this.element.find("div.preview");
		},
		
		adjustHeight: function() {
			var buttons = this.buttonsDiv();
			var buttonsHeight = buttons.is(":visible") ? buttons.height() : 0;
			
			this.setDialogHeight(
				_initialHeight + 
				this.previewDiv().height() +
				buttonsHeight +
				5);
		},
		
		onPreviewUpdate: function() {
			this.buttonsDiv().show();
			this.adjustHeight();
			piggydb.widget.imageViewer.close();
		}
		
	}, module.FragmentFormBase.prototype);
	
	module.FileForm = _class;
	
})(piggydb.widget);	
