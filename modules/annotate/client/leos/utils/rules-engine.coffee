"use strict";

module.exports = class RulesEngine
  _getNodeExplicitType = (nodeType) ->
    switch nodeType
      when Node.TEXT_NODE
        return 'text'
      when Node.ELEMENT_NODE
        return 'element'
      else
        return 'other'
    return

  processElement: (engineRules, element, args...) ->
    @processChildren(engineRules, element, args...)
    _applyRules(engineRules, element, args...)

  processChildren: (engineRules, element, args...) ->
    childNodes = Array::slice.call(element.childNodes)
    if childNodes and childNodes.length > 0
      childNodes.forEach ((node) ->
        @processElement engineRules, node, args...
        return
      ), this

  _applyRules= (engineRules, element, args...) ->
    rules = engineRules[_getNodeExplicitType(element.nodeType)]
    if (rules?)
      for rule of rules 
        try
          if (rule != '$' and element.matches(rule)) #using rule name as selector
            rules[rule].apply(element, args)
        catch e
          console.log("Error in rule: #{rule.toString()} - #{e}");
        #calling default rule $
        if (rules.$?)
          rules.$.apply(element, args);
