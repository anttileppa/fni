(function() {
  'use strict';
  
  /* Image popups */
  
  function initializeImagePopups() {
    $('.gamelibrary-publication').each(function (publicationIndex, publication) {
      var galleryItems = new Array();
      
      $(publication).find('.gamelibrary-publication-thumbnails-container img').each(function (thumbnailIndex, thumbnail) {
        galleryItems.push({
          src: $(thumbnail).data('url')
        });
      });
      
      if (galleryItems.length == 0) {
        // There is only one image and thus no thumbnails so we need to
        // use the publication image
        
        var url = $(publication).find('.gamelibrary-publication-image-container img').data('url');
        if (url) {
          galleryItems.push({
            src: url
          });
        }
      }
      
      $(publication).find('.gamelibrary-publication-images-container a').magnificPopup({ 
        type: 'image',
        gallery: {
          enabled: true
        },
        items: galleryItems
      });
    });
  };
  
  /* Search */
  
  function initializeSearch() {
    $("#gamelibrary-search").autocomplete({
      source : function(request, response) {
        $.ajax({
          url : CONTEXTPATH + "/gamelibrary/publicationSearch/",
          data : {
            q: request.term
          },
          success : function(data) {
            response($.map(data, function(item) {
              console.log(item);
              
              return {
                label : item.name,
                value : item.link
              };
            }));
          }
        });
      },
      minLength : 1,
      select : function(event, ui) {
        if (ui.item) {
          event.preventDefault();
          window.location.href = CONTEXTPATH + '/' + ui.item.value;
        }
      },
      open : function() {
        $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
      },
      close : function() {
        $(this).removeClass("ui-corner-top").addClass("ui-corner-all");
      }
    });
  }
  
  /* Jsf events */
  
  window.onJsfCategoryChange = function (event) {
    if (event.status == 'success') {
      initializeImagePopups(); 
    }
  };
  
  /* Jsf Actions */
  
  /**
   * Publication thumbnails / Mouse Enter
   */
  
  $(document).on('mouseenter', '.gamelibrary-publication-thumbnails-container img', function (e) {
    var publication = $(this).closest('.gamelibrary-publication');
    var publicationId = publication.data('publication-id');
    var imageUrl = $(this).data('url');
    var thumbnailUrl = imageUrl + '?width=128&height=128';
   
    $('.gamelibrary-publication[data-publication-id="' + publicationId + '"] .gamelibrary-publication-image-container a').attr("href", imageUrl);
    $('.gamelibrary-publication[data-publication-id="' + publicationId + '"] .gamelibrary-publication-image-container img').attr("src", thumbnailUrl);
  });
  
  $(document).ready(function () {
    initializeImagePopups();
    initializeSearch();
  });
  
}).call(this);