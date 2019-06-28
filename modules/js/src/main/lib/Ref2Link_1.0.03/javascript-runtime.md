# Ref2link's javascript runtime

### Pre-requisites
* [jQuery v1.10+ http://jquery.com/]

When using tooltips, it will also require
* [bootstrap v3.x https://getbootstrap.com/] with alert, dropdown components and the grid CSS
* [Font Awesome v4.x http://fontawesome.io/] for tooltip icons

### Usage 

A script must be added to web pages that use it after jQuery is included
 
    <script type="text/javascript" src="[path/to/reftolink/subdir/]lib/jquery-parsetext.min.js?ruleenvironment=..."></script>
    
ruleenvironment parameter is optional and serves to control which rules are used (see Controlling which rules to use)

Then to parse a given text use:

    <script type="text/javascript">
        $('jquery selector of the container holding the parsable text').parseDefered();// parses references asynchronously to be used in most cases
        $('jquery selector of the container holding the parsable text').parseReferences();// parse the text synchronyously 
    </script>

    
#### Controlling which rules to use
By default Ref2link will use rules pointing to public resources (eurlex, curiaj, etc).
To enable other known rules:

    <script type="text/javascript">
        $.fn.ref2link.setFilter('environment', ['SOLON-PRD', ...]);// enable sets of rules
    </script>

Public rules will always be used (see reference documentation for ways around it)

#### Disabling the tooltip
Some rules define multiple valid views that take the user to different resources. 
By default ref2link will show a popup when the user hovers the default rendered reference.
To disable the tooltip, use the following snipplet before the $().parseDefered() is used:
 
    <script type="text/javascript">
        $.fn.ref2link.options = {tooltipTrigger: 'notooltip'};
    </script>
    
### Known issues and limitations

The whole text of the node (markup included) is parsed and, when done, the result is re-inserted back in the node.
Based on where the reference is located, some issues might arise (usage in HTML attributes is an example as it will try to insert some markup in that attribute).
If the container has stateful elements, (like form inputs, bounded events) they are NOT ALWAYS fully restored.
To work around this issues use:

    <script type="text/javascript">
        var $container = $('jquery selector of the container holding the parsable text'),
            references = $container.getReferences(), // an array of objects containing information about references; see reference documentation for the exact description of the object)
            textNodes = $container.contents().filter(function() { return this.nodeType == 3; })
        ;
        
        $(textNodes).each(function() {
            var self = this;
            $.each(references, function() {
                self.nodeValue = self.nodeValue.replace(this.wholeMatch, this.views[0]);
            });
        });
    </script>
