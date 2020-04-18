Plugin = require('../plugin')
$ = require('jquery')

LEOS_config = require('../../../leos/shared/config');

makeButton = (item) ->
  anchor = $('<button></button>')
  .attr('href', '')
  .attr('title', item.title)
  .attr('name', item.name)
  .on(item.on)
  .addClass('annotator-frame-button')
  .addClass(item.class)
  button = $('<li></li>').append(anchor)
  return button[0]

module.exports = class Toolbar extends Plugin
  HIDE_CLASS = 'annotator-hide'

  events:
    'setVisibleHighlights': 'onSetVisibleHighlights'
    'LEOS_setVisibleGuideLines': 'onSetVisibleGuideLines'

  html: '<div class="annotator-toolbar"></div>'

  pluginInit: ->
    localStorage.setItem('shouldAnnotationTabOpen', true)

    @annotator.toolbar = @toolbar = $(@html)
    if @options.container?
      $(@options.container).append @toolbar
    else
      $(@element).append @toolbar

    items = [
      "title": "Close Sidebar"
      "class": "annotator-frame-button--sidebar_close h-icon-close"
      "name": "sidebar-close"
      "on":
        "click": (event) =>
          event.preventDefault()
          event.stopPropagation()
          @annotator.hide()
          @toolbar.find('[name=sidebar-close]').hide();
    ,
      "title": "Toggle or Resize Sidebar"
      "class": "annotator-frame-button--sidebar_toggle h-icon-chevron-left"
      "name": "sidebar-toggle"
      "on":
        "click": (event) =>
          event.preventDefault()
          event.stopPropagation()
          collapsed = @annotator.frame.hasClass('annotator-collapsed')
          if collapsed
            state = @annotator.visibleGuideLines = @annotator.visibleHighlights
            @annotator.setAllVisibleGuideLines state
            @annotator.show()
            localStorage.setItem('shouldAnnotationTabOpen', true)
          else
            @annotator.hide()
            localStorage.setItem('shouldAnnotationTabOpen', false)
            state = @annotator.visibleGuideLines = false
            @annotator.setAllVisibleGuideLines state
    ,
      "title": "Hide Highlights"
      "class": "h-icon-visibility"
      "name": "highlight-visibility"
      "on":
        "click": (event) =>
          event.preventDefault()
          event.stopPropagation()
          state = not @annotator.visibleHighlights
          @annotator.setAllVisibleHighlights state
          if !state || _this.annotator.frame.width() >= LEOS_config.FRAME_DEFAULT_MIN_WIDTH
            @annotator.setAllVisibleGuideLines state
    ,
#      LEOS change 3630
      "title": "Hide Line Guides"
      "class": "hide-guidelines-icon"
      "name": "lineguide-visibility"
      "on":
        "click": (event) =>
          if _this.annotator.frame.width() < LEOS_config.FRAME_DEFAULT_MIN_WIDTH
            state = @annotator.visibleGuideLines = false
          else
            state = not @annotator.visibleGuideLines
          event.preventDefault()
          event.stopPropagation()
          @annotator.setAllVisibleGuideLines state
    ,
#     LEOS change 3632
      "title": "New Document Note"
      "class": "h-icon-note"
      "name": "insert-comment"
      "on":
        "click": (event) =>
          event.preventDefault()
          event.stopPropagation()
          @annotator.createAnnotation()
          @annotator.show()
    ]

    @annotator.visibleHighlights = true
    state = @annotator.visibleGuideLines = false
    @annotator.setAllVisibleGuideLines state

    @buttons = $(makeButton(item) for item in items)

    list = $('<ul></ul>')
    @buttons.appendTo(list)
    @toolbar.append(list)

    # Remove focus from the anchors when clicked, this removes the focus
    # styles intended only for keyboard navigation. IE/FF apply the focus
    # psuedo-class to a clicked element.
    @toolbar.on('mouseup', 'a', (event) -> $(event.target).blur())

  onSetVisibleHighlights: (state) ->
    if state
      $('[name=highlight-visibility]')
      .removeClass('h-icon-visibility-off')
      .addClass('h-icon-visibility')
      .prop('title', 'Hide Highlights');
    else
      $('[name=highlight-visibility]')
      .removeClass('h-icon-visibility')
      .addClass('h-icon-visibility-off')
      .prop('title', 'Show Highlights');

  onSetVisibleGuideLines: (state) ->
    if state
      $('[name=lineguide-visibility]')
      .removeClass('hide-guidelines-icon')
      .addClass('show-guidelines-icon')
      .prop('title', 'Hide Guide Lines');
    else
      $('[name=lineguide-visibility]')
      .removeClass('show-guidelines-icon')
      .addClass('hide-guidelines-icon')
      .prop('title', 'Show Guide Lines');

  disableMinimizeBtn: () ->
    $('[name=sidebar-toggle]').remove();

  disableHighlightsBtn: () ->
    $('[name=highlight-visibility]').remove();

  disableNewNoteBtn: () ->
    $('[name=insert-comment]').remove();

  disableCloseBtn: () ->
    $('[name=sidebar-close]').remove();

  disableGuideLinesBtn: () ->
    $('[name=lineguide-visibility]').remove();

  getWidth: () ->
    return parseInt(window.getComputedStyle(this.toolbar[0]).width)

  hideCloseBtn: () ->
    $('[name=sidebar-close]').hide();

  showCloseBtn: () ->
    $('[name=sidebar-close]').show();

  showCollapseSidebarBtn: () ->
    $('[name=sidebar-toggle]')
    .removeClass('h-icon-chevron-left')
    .addClass('h-icon-chevron-right')

  showExpandSidebarBtn: () ->
    $('[name=sidebar-toggle]')
    .removeClass('h-icon-chevron-right')
    .addClass('h-icon-chevron-left')
