#### Include the javascript tool
There are two variants of the javascript tool:
 * the reference tool _jquery-parsetext.js_
 * the static tool _jquery-parsetext.min.js_
 
Difference is that the first one needs the rules to be injected from somewhere else and the later has all the rules embedded.
The min version is generated everytime re2link is reloaded

Both of these files can be used in different contexts: internet, intranet, local net/files with relative or absolute url.
Examples:

    <script type="text/javascript" src="reftolink/jquery-parser[.min].js"></script><!-- works in any context -->
    <script type="text/javascript" src="//webgate.ec.testa.eu/ref2link/jquery-parse[.min].js"></script><!-- works in any context; official(?) distribution -->
    <script type="text/javascript" src="file:///X:/ref2link/jquery-parse[.min].js"></script><!-- works only on local files -->
    
#### Include the reference javascript tool (TODO: move to basic usage)
The reference/static reference tool can be included

    <script type="text/javscript" src="//sjdapps2/solon/ref2link/jquery-parsetext[.min].js?[parameters]"></script>
    
##### Parameters for filtering***
Through the url query 
* ruleenvironment(alias re) - used to restrict applicable rules to certain environments comma separated (ex: re=#application.thisinstance#,DIGIT-PRD , re=HOUSING-DEV)
* ruletarget(alias rt) - used to restrict applicable rules to certain targets (ex: rt=curiaj.jurisp,curia.jurisp.detail -> only rules with views targeted will be rendered)
* ruletype(alias rr) - used to restrict applicable rules to certain rule types (ex: rr=curiaj,eurlex -> only the curiaj and eurlex rules will be applied)
* TODO: rulefilter(alias rf) - base64 encoded version of _[re=...][&][rt=...][&][rr=...]_ or any other combination of the above filters (used to obfuscate what filters are used when the file is shared with other DGs)

Through the tool API

    $.fn.ref2link.setFilter('environments', ['HOUSING-DEV', 'DIGIT-PRD', 'ANOTHER-DG-PRD']);
    $.fn.ref2link.setFilter('target', ['curiaj.jurisp', 'curia.jurisp.detail']);
    $.fn.ref2link.setFilter('type', ['curiaj', 'eurlex']);
    
***These 3 filters can be combined 

## Javascript API

### Plugin methods
* $().fastParseText(rule) - will apply the rule to all matching nodes (DEPRECATED; use parseReferences instead)
* $().parseReferences() - will parse all filtered rules on all matching nodes and replace references with view
* $().parseDeferred([rules=runtimeRules]) - will asynchronously parse all specified rules on all matching nodes; each parsing request is added to the queue and will be processed when all prior requests finish processing
* $().parseTextRules([rules=runtimeRules]) - Backwords compatible alias of parseDeferred
* $().unparseTextRules() - will remove all generated links and restore initial text in their place
* $().getRef2linkMatch() - will get the parsing data for a rendered view
* $().getRef2linkMatch(field) - will get the field from the parsing data of a rendered view
* $().getParsedMatch() - will first parse the node (if not already done) and return $().getRef2linkMatch() of that node
* $().getReferences() - returns an object where the key is the reference and the value is an object containing matching data
* $().getFormattedReferences([format='identity']) - returns matching data formatted using format; format is one of the formatters (see $.fn.ref2link.formatters) or a callback
* $().getReferenceInfo() - backward compatible method to get matching references into an array
* $.fn.ref2link.applyRule(text, rule) - will apply all valid views of the rule to the text
* $.fn.ref2link.addRules(newRules) - adds the rules to the current rule set
* $.fn.ref2link.getRules() - gets all rules that match the set filters (so called runtime rules)
* $.fn.ref2link.simpleParse(template, data) - method to parse a template anchors with keys from data
* $.fn.ref2link.compileGlobalRule(rules) - creates a single pattern from all the passed rules; this pattern is then used to match after which the real rules are applied
* $.fn.ref2link.applyGlobalRule(text) - will apply the rule created by compileGlobalRule and for each match identifies the matching rule after which the rule is applied only to the matching text

* $.fn.ref2link.setFilter(searchField, searchValue) - searchField is one of the 3 filtering fields and searchValue is an array
* $.fn.ref2link.resetFilters() - will reset filters to values defined in the initial script arguments (see [#Parameters for filtering])
* $.fn.ref2link.getGlobalEnvironments() - get all the defined environments
* $.fn.ref2link.getGlobalTypes() - all defined rule types
* $.fn.ref2link.getGlobalTargets() - all defined view targets

#### Internal methods and functions
* $.fn.ref2link.bindTooltips() - binds keyboard/mouse events to ref2link tooltips; IT'S NOT CALLED UNTIL SOMETHING IS PARSED
* $.fn.ref2link.positionHandler() - updates tooltip's position when the viewport changes
* $().reverse - reverses the order of matched elements
* $.fn.ref2link.compileRule(rule) - deserializes and normalizes raw rules
* $.fn.ref2link.stopEvent(event) - stops all propagations and default action of the event
* $.fn.ref2link.regExpEscape(pattern) - will escape with \ all characters that have meaning in regexp context
* $.fn.ref2link.clearCache() - clear internal cache

### Plugin members
* $.fn.ref2link.version - javascript tool's internal version
* $.fn.ref2link.info - markup containing information about ref2link (used in system information boxes on LSDF projects) 
* $.fn.ref2link.filters - object containing set filters
* $.fn.ref2link.defaultFilters - an object containing default filters
* $.fn.ref2link.errors - a list of all rules errors (DEPRECATED)
* $.fn.ref2link.converters - a hash with modifiers that the view template language understands
* $.fn.ref2link.converters.pad(string, paddingString, minimumLength, [position=left]) - pads the string with paddingString up to minimumLength in the left or right position 
* $.fn.ref2link.converters.year(string) - transforms short year to long year format (year lower than 57 will be prefixed with 20, while the rest with 19)
* $.fn.ref2link.converters.trim(string, [characters=allTrimCharacters]) - will remove all characters from start or end position
* $.fn.ref2link.converters.upper(string) - returns string with all characters upper cased
* $.fn.ref2link.converters.lower(string) - returns string with all characters lowered cased
* $.fn.ref2link.converters.replace(string, whatToReplace, withWhatToReplaceWith)
* $.fn.ref2link.converters.length(object) - get the string length or the array length
* $.fn.ref2link.converters.split(string, delimiter) - returns an array of pieces delimited by the delimiter; delimiter can be a regexp
* $.fn.ref2link.converters._default(string, defaultString) - return string if not empty or defaultString otherwise
* $.fn.ref2link.converters.number__from__roman(string) - return the natural representation of a roman number
* $.fn.ref2link.formatters - a hash map with callbacks used to export the matched references in different formats; callback takes two arguments: the references and the text and must (when used with getFormattedReferences) return a structure containing:

      {
          result: <mixed value>,
          type: <mime type>,
          ext: <exported file extension>,
      }
    
    - identity (default if format is empty) - exports an array of references
    - ref2table - exports an XML document representing the references
    - html - exports the text with links to resources embedded
    - email - exports the text with links embedded as URLS (plain text emails style; somewhat similar to Wiki markup)
    - book - TODO: export the text with references at the bottom of the text (book references style)
    
* $.fn.ref2link.triggers - contains default options for all 3 ways of displaying the tooltip:
    - shift+click - triggers the tooltip on shift+click on a ref2link (used in the tool)
    - mouseenter - triggers the tooltip upon hovering a ref2link
    - notooltip - does not display tooltips at all
    
* $.fn.ref2link.editOptions - options used by the tool (refers to shift+click mode)
* $.fn.ref2link.viewOptions - options used in general (refers to mouseenter mode)
* $.fn.ref2link.options - one of editOptions, viewOptions or custom options (runtime options)
* $.fn.ref2link.defaultOptions - viewOptions
* $.fn.ref2link.linkClassName - class that will be added to all the generated links (defaults to empty; except for this class, .ref2link-generated is always added)
* $.fn.ref2link.viewUsesTarget - if true all generated links will have the target="_blank" attribute set
* $.fn.ref2link.viewTitlePrefix - text that will be prepended to the generated link's title attribute
* $.fn.ref2link.viewTitleSuffix - text that will be appended to the generated link's title attribute
    
#### Events
* _parsed.ref2link_ is thrown on the document element when the parsing has been completed globally (all passed nodes are processed)
* _before-replace.ref2link_ is thrown on each node passed to the parser, just before the content is replaced with the parsed one
* _after-replace.ref2link_ is thrown on each node passed to the parser, just after the content is replaced with the parsed one

#### Promises (async processing)
Each call to $().parseDeferred() will return an array of [jQuery.Deferred https://api.jquery.com/category/deferred-object/], one for each matching nodes:
Each promise is resolved with true (if the content was parsed the first time) and with false (if the content was parsed before, maybe another node with the same content or the content was un-parsed) and is rejected with false when the node was parsed already (and not un-parsed).
    
#### Internal members and variables
* $.fn.ref2link.globalMatches - a hash map using the matched as the key and the ref2link object as the value
* $.fn.ref2link.globalViews - a mapping object to facilitate ref2links that use a different string than the whole match; it's used to restore text on un-parsing action
* $.fn.ref2links.globalRule - a rule derived from all the runtime rules
* $.fn.ref2link.getFiltersWithDependencies

