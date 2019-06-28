# Do LEOS specific actions in this class
$ = require('jquery')

Sidebar = require('../../src/annotator/sidebar')
HostBridgeManager = require('./host-bridge-manager')
LeosSuggestionSelection = require('./anchoring/leos-suggestion-selection')
require('./leos-adder')

FRAME_DEFAULT_WIDTH = 348;

module.exports = class LeosSidebar extends Sidebar
  constructor: (element, config) ->
    hypothesisUrl = new URL(config.clientUrl)
    @contextRoot = hypothesisUrl.pathname.substring(0, hypothesisUrl.pathname.indexOf("/",2));

    @options.Document.leosDocumentRootNode = config.leosDocumentRootNode
    @options.Document.annotationContainer = config.annotationContainer
    @options.Document.allowedSelectorTags = config.allowedSelectorTags
    @options.Document.editableSelector = config.editableSelector
    @options.Document.notAllowedSuggestSelector = config.notAllowedSuggestSelector
    @options.Document.readOnly = config.readOnly
    #Set the scrollable element on which the bucket bar should be synced
    @options.BucketBar.scrollables = ["#{@options.Document.annotationContainer}"]

    #Get first non scrollable parent of the container to get an element on which side bar can be fixed
    @container = $(@options.Document.annotationContainer)
    element = _nearestNonScrollableAncestor @container[0]

    self = this
    @container[0].addEventListener("annotationRefresh", () -> _refresh.call(self))
    @container[0].addEventListener("annotationSidebarResize", () -> _setFramePosition.call(self))

    config.docType = 'LeosDocument'

    super

    @hostBridgeManager = new HostBridgeManager(@container[0].hostBridge, @crossframe)
    @leosSuggestionSelection = new LeosSuggestionSelection(@options.Document.allowedSelectorTags, @options.Document.editableSelector, @options.Document.notAllowedSuggestSelector)
    @crossframe.on 'requestAnnotationAnchor', (annot, callback) =>
      suggestionPositionSelector = self.leosSuggestionSelection.getSuggestionSelectors(annot, self.anchors)
      if suggestionPositionSelector?
        callback(null, suggestionPositionSelector)
      else
        callback("No available highlights")

    @adderCtrl.extend(@adder[0], @container[0].hostBridge, {onComment: onComment.bind(self), onSuggest: onSuggest.bind(self), onHighlight: onHighlight.bind(self)})

    this.on 'panelReady', =>
      _setFrameStyling.call(this, element)

  _onSelection: (range) ->
    if (@options.Document.readOnly == "true")
      return
    isAllowed = @leosSuggestionSelection.isSelectionAllowedForSuggestion(range.cloneContents(), range.commonAncestorContainer)
    if (isAllowed)
      @adderCtrl.enableSuggestionButton()
    else
      @adderCtrl.disableSuggestionButton()
    super

  getDocumentInfo: ->
    self = this
    getInfoPromise = super
    return getInfoPromise.then (docInfo) ->
      return self.plugins.Document.getLeosDocumentMetadata()
        .then (leosMetadata) =>
          if leosMetadata?
            docInfo.metadata.metadata = leosMetadata
          return {
            uri: docInfo.uri,
            metadata: docInfo.metadata,
            frameIdentifier: docInfo.frameIdentifier
          }
        .catch (error) =>
          return {
            uri: docInfo.uri,
            metadata: docInfo.metadata,
            frameIdentifier: docInfo.frameIdentifier
          }

  onSuggest = () ->
    range = @selectedRanges[0]
    if range?
      annotationText = @leosSuggestionSelection.extractSuggestionTextFromSelection(range.cloneContents())

      @createAnnotation({$suggestion: true, tags: ["suggestion"], text: annotationText})
    document.getSelection().removeAllRanges()

  onComment = () ->
    @createAnnotation({$comment: true, tags: ["comment"]})
    document.getSelection().removeAllRanges()

  onHighlight = () ->
    @setVisibleHighlights(true)
    @createAnnotation({$highlight: true, tags: ["highlight"]})
    document.getSelection().removeAllRanges()

  _isScrollable = (element) ->
    element.scrollWidth > element.clientWidth or element.scrollHeight > element.clientHeight

  _nearestNonScrollableAncestor = (element) ->
    parentEl = element
    while parentEl.parentElement
      if !_isScrollable(parentEl)
        break
      parentEl = parentEl.parentElement
    parentEl

  _setFrameStyling = (element) ->
    #Set Sidebar specific styling to stick to the container
    @frame.addClass('leos-sidebar')
    _setFramePosition.call(this)

  _setFramePosition = () ->
    position = @element[0].getBoundingClientRect()
    @frame[0].style.top = position.top + "px"
    @frame[0].style.height = position.height + "px"
    @frame[0].style.left = if _isScrollbarVisible then position.right - 18 + "px" else position.right + "px"

  _isScrollbarVisible = () ->
    return @container[0].scrollHeight > @container[0].clientHeight

  _refresh = () ->
    self = this
    # A list of annotations that need to be refreshed.
    refreshAnnotations = new Set
    # Find all the anchors that have been invalidated by page state changes.
    # Would be better to loop on all existing annotations, but didn't find any other way to access annotations from sidebar
    if @anchors?
      for anchor in @anchors when anchor.highlights?
        # The annotations for these anchors need to be refreshed.
        # Remove highlights in the DOM
        for h in anchor.highlights when h.parentNode?
          $(h).replaceWith(h.childNodes)
        delete anchor.highlights
        delete anchor.range
        # Add annotation to be anchored again
        refreshAnnotations.add(anchor.annotation)

      refreshAnnotations.forEach (annotation) ->
        @anchor(annotation)
      , self
    
    self.crossframe?.call('reloadAnnotations')

  _cleanup = () ->
    # TODO : Temporary solution to cleanup references to annotation scripts 
    quote = (regex) ->
      regex.replace(/([()[{*+.$^\\|?\/])/g, '\\$1')

    annotateScripts = document.querySelectorAll("script[src*=#{quote(@contextRoot)}]")
    annotateScripts.forEach((annotateScript) ->
      annotateScript.parentNode.removeChild(annotateScript)
    )

    annotateLinks = document.querySelectorAll("link[href*=#{quote(@contextRoot)}]")
    annotateLinks.forEach((annotateLink) ->
      annotateLink.parentNode.removeChild(annotateLink)
    )

    configScripts = document.querySelectorAll("script.js-hypothesis-config")
    configScripts.forEach((configScript) ->
      configScript.parentNode.removeChild(configScript)
    )

  show: () ->
    super
    if @frame.width() != @frameCurrentWidth and @frame.width() != 0
      @frameCurrentWidth = @frame.width() 
    @frame.css 'margin-left': "#{-1 * @frameCurrentWidth}px"
    @frame.css 'width': "#{@frameCurrentWidth}px"

  hide: () ->
    super
    if @frameCurrentWidth? and @frame.width() != 0
      @frameCurrentWidth = @frame.width()
    else
      @frameCurrentWidth = FRAME_DEFAULT_WIDTH
    @frame.css 'width': "0px"

  destroy: () ->
    super
    _cleanup.call(this)

#  normalizeURI = (uri, baseURI) ->
# use if required