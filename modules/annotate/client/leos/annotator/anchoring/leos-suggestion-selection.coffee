$ = require('jquery')
RulesEngine = require('../../utils/rules-engine')

# extends string indexOf functionnality 
String::nthIndexOf = (pattern, n) ->
  i = -1
  while n-- and i++ < @length
    i = @indexOf(pattern, i)
    if i < 0
      break
  i

escape = (string) ->
  string.replace /([ #;&,.+*~\':"!^$[\]()=>|\/])/g, '\\$1'

module.exports = class LeosSuggestionSelection
  HIGHLIGHT_TAG = "hypothesis-highlight"

  constructor: (allowedSelectorTags, editableAttribute, notAllowedSuggestSelector) ->
    @allowedSelectorTags = allowedSelectorTags
    @editableAttribute = editableAttribute
    @notAllowedSuggestSelector = notAllowedSuggestSelector

  #
  # Returns true if selection text contains only text or allowed elements
  # @param selection's range
  # @return: returns true if selection text contains only text or allowed elements.
  #
  isSelectionAllowedForSuggestion: (documentFragment, rangeCommonAncestorContainer) ->
    self = this
    treeWalker = document.createTreeWalker(documentFragment, NodeFilter.SHOW_ELEMENT, { acceptNode: (node) ->
      if ((node.nodeType == Node.TEXT_NODE) or (node.nodeType == Node.ELEMENT_NODE and (node.matches(self.allowedSelectorTags) or node.matches(HIGHLIGHT_TAG))))
        NodeFilter.FILTER_SKIP
      else
        NodeFilter.FILTER_ACCEPT
    }, false)
    return (treeWalker.nextNode() == null and _isEditable(rangeCommonAncestorContainer, self.editableAttribute) and _isAllowed(rangeCommonAncestorContainer, self.notAllowedSuggestSelector))

  #
  # Returns true if selection is done on an editable part
  # @param element: range common ancestor
  # @param editableAttribute: attribute name which define if an element is editable or not
  # @return: returns true if selection is done on an editable part.
  #
  _isEditable = (element, editableAttribute) =>
    if editableAttribute == ""
      return true 

    try
      editableElement = $(element).closest("[#{escape(editableAttribute)}]")
    catch error
      return false

    if editableElement? and editableElement.length > 0 and editableElement.attr(editableAttribute) == "true"
      return true
    else
      return false

  #
  # Returns true if selection for suggestion is allowed - checks the parent's elements
  # @param element: range common ancestor
  # @param notAllowedSuggestSelector: attribute containing JQuery selector of not allowed parent elements
  # @return: returns true if selection is allowed.
  #
  _isAllowed = (element, notAllowedSuggestSelector) =>
    try
      notAllowedSuggestElement = $(element).closest(notAllowedSuggestSelector)
    catch error
      return true

    if notAllowedSuggestElement? and notAllowedSuggestElement.length > 0 
      return false
    else
      return true

  #
  # Extracts from selection the content in Html removing allowed elements tags
  # @param selection's range document fragment
  # @return: returns from selection the content in Html removing allowed elements tags.
  #
  extractSuggestionTextFromSelection: (documentFragment) ->
    self = this
    rulesEngine = new RulesEngine()
    tmpDiv = document.createElement("div")
    tmpDiv.appendChild(documentFragment)
    selectionsRules = element:
      "#{self.allowedSelectorTags}": ->
        @insertAdjacentHTML 'afterend', @innerHTML
        @parentNode.removeChild this
        return
      "#{HIGHLIGHT_TAG}": ->
        @insertAdjacentHTML 'afterend', @innerHTML
        @parentNode.removeChild this
        return

    rulesEngine.processElement selectionsRules, tmpDiv
    return tmpDiv.innerHTML

  #
  # While accepting a suggestion (@param annot) it generates appropriated array of selectors to send to LEOS
  # @param suggestion: suggestion itself 
  # @param anchors: all anchors of the document 
  # @return: returns array of selectors, one selector contains {origText: this.textContent, newText: newHighlightText, elementId: elementId, startOffset: startOffset, endOffset: endOffset}.
  #
  # FIXME temporary fix we consider that only one highlight will be used here 
  getSuggestionSelectors: (suggestion, anchors) ->
    anchor = anchors.find (a) =>
      a.annotation.$tag == suggestion.$tag
    if anchor?
      highlightedText = "";
      if anchor.highlights?
        anchor.highlights.every (h) =>
          highlightedText += h.textContent
        parentElement = _getClosestAncestorWithId anchor.range.commonAncestorContainer #Common Ancestor could a highlight or a wrapper, find appropriate one
        childHighlights = parentElement.querySelectorAll(HIGHLIGHT_TAG)
        hIndex = Array.prototype.indexOf.call(childHighlights, anchor.highlights[0]);
        eltContent = parentElement.innerHTML
        startOffset = eltContent.nthIndexOf("<#{HIGHLIGHT_TAG}", hIndex + 1) #Takes only the first one: temporary fix because until now only one text node could highlighted
        endOffset = startOffset + highlightedText.length
        elementId = parentElement.id
        return {origText: highlightedText, elementId: elementId, startOffset: startOffset, endOffset: endOffset}
      else
        return null
    else
      return null

  #
  # It gets the closest parent of @param element with a non null/empty id
  # @param element
  # @return: returns the closest parent of @param element with a non empty id or returns null if not found.
  #
  _getClosestAncestorWithId = (element) ->
    if (element? and element.id? and element.id != "")
      return element
    else if (element.parentElement?)
      return _getClosestAncestorWithId(element)
    else
      return null