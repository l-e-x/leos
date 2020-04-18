'use strict';

var angular = require('angular');

var events = require('../events');

function getExistingAnnotation(store, id) {
  return store.getState().annotations.find(function (annot) {
    return annot.id === id;
  });
}

//LEOS Change
function LEOS_processAnnotations(annotations, _rootScope) {
  annotations.forEach(function (annotation) {
    annotation.group = annotation.group.replace(" ",_rootScope.ANNOTATION_GROUP_SPACE_REPLACE_TOKEN);
  });
}

// Wraps the annotation store to trigger events for the CRUD actions
// @ngInject
function annotationMapper($rootScope, store, api) {
  function loadAnnotations(annotations, replies) {
    annotations = annotations.concat(replies || []);
    //LEOS Change : remove white spaces from GROUP names
    LEOS_processAnnotations(annotations, $rootScope);

    var loaded = [];
    annotations.forEach(function (annotation) {
      var existing = getExistingAnnotation(store, annotation.id);
      if (existing) {
        $rootScope.$broadcast(events.ANNOTATION_UPDATED, annotation);
        return;
      }
      loaded.push(annotation);
    });

    $rootScope.$broadcast(events.ANNOTATIONS_LOADED, loaded);
  }

  function unloadAnnotations(annotations) {
    var unloaded = annotations.map(function (annotation) {
      var existing = getExistingAnnotation(store, annotation.id);
      if (existing && annotation !== existing) {
        annotation = angular.copy(annotation, existing);
      }
      return annotation;
    });
    $rootScope.$broadcast(events.ANNOTATIONS_UNLOADED, unloaded);
  }

  function createAnnotation(annotation) {
    $rootScope.$broadcast(events.BEFORE_ANNOTATION_CREATED, annotation);
    return annotation;
  }

  function deleteAnnotation(annotation) {
    return api.annotation.delete({
      id: annotation.id,
    }).then(function () {
      $rootScope.$broadcast(events.ANNOTATION_DELETED, annotation);
      return annotation;
    });
  }

  function flagAnnotation(annot) {
    return api.annotation.flag({
      id: annot.id,
    }).then(function () {
      $rootScope.$broadcast(events.ANNOTATION_FLAGGED, annot);
      return annot;
    });
  }

  return {
    loadAnnotations: loadAnnotations,
    unloadAnnotations: unloadAnnotations,
    createAnnotation: createAnnotation,
    deleteAnnotation: deleteAnnotation,
    flagAnnotation: flagAnnotation,
  };
}


module.exports = annotationMapper;
