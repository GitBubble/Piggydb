piggydb.namespace("piggydb.util", {
	
	escapeHtml: function(str) {
		if (!str) return str;
	  return str.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
	},
	
	isBlank: function(str) {
	  if (str == null) return true;
	  if (jQuery.trim(str) == "") return true;
	  return false;
	},
	
	isNotBlank: function(str) {
	  return !isBlank(str);
	},
	
	addHiddenValue: function(form, name, value) {
    jQuery('<input type="hidden" name="' + name + '" value="' + value + '"/>').appendTo(form);
	},
	
	toggleTreeNode: function(node) {
	  var node = jQuery(node).closest("li");
	  var className = node.attr("class");
	  node.removeAttr("class");
	  if (className.match("^collapsed")) {
	    node.addClass(className.replace("collapsed", "expanded"));
	    node.children("ul").show();
	  }
	  else if (className.match("^expanded")) {
	    node.addClass(className.replace("expanded", "collapsed"));
	    node.children("ul").hide();
	  }
	},
	
	cumulativeOffsetTop: function(element) {
		var offset = 0;
	  while (true) {
	    offset += element.offsetTop;
	    element = element.offsetParent;
	    if (!element) break;
	  }
	  return offset;
	},
	
	setScrollTopTo: function(id) {
	  var targets = jQuery('#' + id);
	  if (targets.size() == 0) return;
	  var offset = cumulativeOffsetTop(targets[0]);
	  jQuery("html, body").scrollTop(offset);
	},
	
	// for ul.liquid-blocks / li.liquid-block
	liquidBlocks: function(baseElement, blockWidth, containerWidth) {
	  var blocks = baseElement.find("ul.liquid-blocks");

	  // Get the width of row
	  if (containerWidth == null) {
	    // Reset the container size to a 100% once view port has been adjusted
	  	blocks.css({ 'width' : "100%" });
	    containerWidth = blocks.width();
	  }

	  // Find how many blocks can fit per row
	  // then round it down to a whole number
	  var colNum = Math.floor(containerWidth / blockWidth);
	  if (colNum == 0) colNum = 1;

	  // Get the width of the row and divide it by the number of blocks it can fit
	  // then round it down to a whole number.
	  // This value will be the exact width of the re-adjusted block
	  var colFixed = Math.floor(containerWidth / colNum);

	  // Set exact width of row in pixels instead of using %
	  // Prevents cross-browser bugs that appear in certain view port resolutions.
	  blocks.css({ 'width' : containerWidth });

	  // Set exact width of the re-adjusted block
	  blocks.find("li.liquid-block").css({ 'width' : colFixed });
	},
	
	clickSelectSwitch: function(button) {
		button = jQuery(button);
	  if (button.hasClass("selected")) return false;
	  button.siblings("button.selected").removeClass("selected");
	  button.addClass("selected");
	  return true;
	},
	
	blockPageDuringAjaxRequest: function() {
		jQuery(document).ajaxStop(jQuery.unblockUI); 
		jQuery.blockUI({ 
			message: '<img src="images/load-large.gif" border="0"/>',
			css: { 
				border: '0px solid #aaa',
				width: '30px',
				padding: '15px',
				left: '45%',
				fadeIn: 0,
				fadeOut: 0
			}
		});
	}
});



piggydb.namespace("piggydb.util.domain", {
	
	tagIconClass: function(tagName, isTagFragment) {
		var c = "tagIcon";
		if (tagName.charAt(0) == "#") {
			c += " tagIcon-system";
			c += " tagIcon-" + tagName.substring(1);
		}
		else if (isTagFragment != null) {
      c += " tagIcon-" + (isTagFragment ? "fragment" : "plain");
    }
		return c;
	},
	
	miniTagIconClass: function(tagName) {
		var c = "miniTagIcon";
		if (tagName.charAt(0) == "#") c = c + " miniTagIcon-" + tagName.substring(1);
		return c;
	},
	
	onDeleteTagClick: function(tagName, form) {
	  form.tagToDelete.value = tagName;
	}
});

