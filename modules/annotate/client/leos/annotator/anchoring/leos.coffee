html = require('../../../src/annotator/anchoring/html')

configFrom = require('../../../src/annotator/config/index')
config = configFrom(window)

{
  LeosAnchor
  FragmentAnchor
  RangeAnchor
  TextPositionAnchor
  TextQuoteAnchor
} = require('./leos-types')

querySelector = (type, root, selector, options) ->
  doQuery = (resolve, reject) ->
    try
      anchor = type.fromSelector(root, selector, options)
      range = anchor.toRange(options)
      resolve(range)
    catch error
      reject(error)
  return new Promise(doQuery)

exports.anchor = (root, selectors, options = {}) ->
  leos = null

  for selector in selectors ? []
    switch selector.type
      when 'LeosSelector'
        leos = selector


  if leos?
    promise = Promise.reject('unable to anchor')
    return promise.catch ->
      return querySelector(LeosAnchor, root, leos, options)
  else 
    return html.anchor(root, selectors, options)

exports.describe = (root, range, options = {}) ->
  types = [LeosAnchor, FragmentAnchor, RangeAnchor, TextPositionAnchor, TextQuoteAnchor]

  selectors = for type in types
    try
      anchor = type.fromRange(root, range, options)
      selector = anchor.toSelector(options)
    catch
      continue

  #LEOS-2789 replace wrappers tags by "//" in xpaths
  if config.ignoredTags?
    tags = config.ignoredTags.join('|')
    matchTags = new RegExp('(' + tags + ')(\\[\\d+\\])?', 'g')
    matchSlashes = new RegExp('\\/(\\/)+\\/', 'g')
    selectors.filter((s) ->
      s.type == 'RangeSelector'
    ).forEach (selector) ->
      selector.endContainer = selector.endContainer.replace(matchTags, '').replace(matchSlashes, '//')
      selector.startContainer = selector.startContainer.replace(matchTags, '').replace(matchSlashes, '//')
  return selectors