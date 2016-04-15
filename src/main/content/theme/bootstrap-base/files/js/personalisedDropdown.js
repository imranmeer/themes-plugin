! function ($) {
    // PERSONALISED STORYLIST JAVASCRIPT - BR
    // Adds addtional the following functionality to a dropdown button
    // Allows mutliple selection from the dropdown list
    // Allows the selection to be stored using localStorage by selecting the confirmation button
    // Retrieves content using Ajax
    // Allows content ot be refreshed

    // Constructor to set up dropdown
    function PersonalisedDropdown(select, options) {
        // Initialization.
        this.options = $.extend({}, this.defaults, options); //Merges the given options with the default options
        // Set up
        this.$container = $(".btn-group"); // define container
        this.$button = $('.dropdown.btn-default'); // define button
        this.$ul = $(".dropdown-menu"); // define dropdown ul
        this.buildDropdownOptions();
        this.setActiveSeletions();
        this.updateButtonText();
    };

    PersonalisedDropdown.prototype = {
        defaults: {
            // set variables
            selectedClass: 'active',
            nonSelectedText: 'None selected',
            nSelectedText: 'selected',
            numberOfStories: 5,
            baseUrl: "undefined",
            storageId:'psl',
            site: 'Blah',
            buttonText: function (options) {// Either returns default nonSelectedText text if no options are selected, a list of selected options if under 4, or nSelectedText and value if 4 or above
                if (options.length === 0) { // no options
                    return this.nonSelectedText + ' <b class="caret"></b>';
                } else {
                    if (options.length > 3) { // more than 3 options
                        return options.length + ' ' + this.nSelectedText + ' <b class="caret"></b>';
                    } else { // 0 < x < 4
                        var selected = '';
                        $(options).each(function (index) { // each selection
                            var label = options[index];
                            if (index == options.length - 1) { // last selected
                                selected += label;
                            } else {
                                selected += label + ', ';
                            }
                        });
                        return selected + ' <b class="caret"></b>';
                    }
                }
            },

            buttonTitle: function (options) {// Updates the title of the button similar to the buttonText function
                if (options.length === 0) { // no options
                    return this.nonSelectedText;
                } else {
                    var selected = '';
                    $(options).each(function (index) {
                        var label = options[index];
                        if (index == options.length - 1) { // last selected
                            selected += label;
                        } else {
                            selected += label + ', ';
                        }
                    });
                    return selected;
                }
            },
        },

        constructor: PersonalisedDropdown,
        buildDropdownOptions: function () { // define dropdown actions
            $('li input', this.$ul).on('change', $.proxy(function (event) { // Bind the change event on the dropdown elements.
                var checked = $(event.target).prop('checked') || false; // set checked value
                if (this.options.selectedClass) { // Apply or unapply the configured selected class.
                    if (checked) { // add active class
                        $(event.target).parents('li')
                            .addClass(this.options.selectedClass);
                    } else { // remove active class
                        $(event.target).parents('li')
                            .removeClass(this.options.selectedClass);
                    }
                }
                var value = $(event.target).val(); // Get the changed option.
                var $option = $('option').filter(function () { //Gets a select option by its value.
                    return $(this).val() === value;
                });
                if (checked) {
                    $option.prop('selected', true); // Select additional option.
                } else {
                    $option.prop('selected', false); // Unselect option.
                }
                this.updateButtonText(); // update button
            }, this));

            $('li a', this.$ul).on('touchstart click', function (event) {
                event.stopPropagation(); // prevents any parent handlers from being notified of the event
            });

            var self = this;
            $($('.personalised-storylist .confirm.btn')).on('touchstart click', function (event) { // when 'choose club' button is pressed
                var selection = self.getActiveSeletions(); // get selected options
                localStorage[self.options.storageId] = selection; // save selected options through local storage
                self.getStorylistContent(selection); // get content for selected options
            });
            $('.personalised-storylist-bottom > a').on('touchstart click', function (event) { // when 'refresh' is pressed
                self.getStorylistContent(self.getActiveSeletions()); // get selected options
                return false;
            });

            // Keyboard support.
            this.$container.on('keydown', $.proxy(function (event) {
                if ($('input[type="text"]', this.$container).is(':focus')) {
                    return;
                }
                if ((event.keyCode === 9 || event.keyCode === 27) && this.$container.hasClass('open')) { // tab or escape.
                    this.$button.click(); // close
                } else {
                    var $items = $(this.$container).find("li:visible a");
                    if (!$items.length) { // no visible items
                        return;
                    }
                    var index = $items.index($items.filter(':focus'));
                    if (event.keyCode === 38 && index > 0) { // up arrow.
                        index--;
                    } else if (event.keyCode === 40 && index < $items.length - 1) { // down arrow.
                        index++;
                    } else if (!~index) {
                        index = 0;
                    }
                    var $current = $items.eq(index);
                    $current.focus();
                    if (event.keyCode === 32 || event.keyCode === 13) { //space or enter
                        var $checkbox = $current.find('input');
                        $checkbox.prop("checked", !$checkbox.prop("checked")); // select option
                        $checkbox.change();
                    }
                    event.stopPropagation(); // prevent parent handlers  being notified
                    event.preventDefault(); // stop default behaviour
                }
            }, this));
        },

        // Update the button text and its title based on the currenty selected options
        updateButtonText: function () {
            var names = this.getActiveSeletionNames(); // get current selections
            $(this.$button, this.$container).html(this.options.buttonText(names)); // Update the displayed button text.
            $(this.$button, this.$container).attr('title', this.options.buttonTitle(names)); // Update the title attribute of the button.
        },

        // Get active selections
        getActiveSeletions: function () {
            var options = $('.dropdown-container li.active');
            var names = [];
            $(options).each(function (index) {
                names.push($.trim($(this).find('input').prop('value'))); // add option to array
            });
            return names;
        },

        // Get active selections
        getActiveSeletionNames: function () {
            var options = $('.dropdown-container li.active');
            var names = [];
            $(options).each(function (index) {
                names.push($.trim($(this).find('label').text())); // add option to array
            });
            return names;
        },

        // set active selections
        setActiveSeletions: function () {
            var selection;
            if (localStorage[this.options.storageId]) { // local storage has previously been used
                selection = localStorage[this.options.storageId]; // get stored values
                var array = selection.split(','); // split into individual slections
                $("li label").each(function (i) { // every option
                    var self = this;
                    $(self).children('input').prop('checked', false); // Deselect all options.
                    $(array).each(function (j) {
                        if ($.trim($(self).children('input').prop('value')) == array[j]) { // if option is on the list
                            $(self).parents('li').addClass('active');
                            $(self).children('input').prop('checked', true); // Select additional option.
                        }
                    });
                });
            } else { // no local storage
                selection = '';
                $("li label").each(function (i) { // every option
                    var self = this;
                    $(self).children('input').prop('checked', false); // Deselect all options.
                });
            }
          this.getStorylistContent(selection); // get content for specified selection
        },

        // Get content for selection using AJAX request - this function has not been tested as there is currently no way of doing this. Therfore the code can be changed in order to retrive and display the content correctly. It has been assumed that the request will return a 'result' object. The result object will be made of a number of 'teaser' objects. The parameters used below are:
        // teaser.headline - The headline of the teaser
        // teaser.label - The category of the teaser, if the widhet is used for football, then this would be the club
        // teaser.mediaIconClass - The icon used to disply the media icon, either Audio - "glyphicon" "glyphicon-volume-up", Video - "glyphicon" "glyphicon-facetime-video", or Photo - "glyphicon" "glyphicon-camera"
        // teaser.updatedTime - How long since the article was last updated

        // The URL used in the request is an example only, it is assumed that the selected options will be passed as a comma seperated list. The number of stories will also be passed as a parameter.
        // If no clubs are sent as parameters for the AJAX request then teasers from all categories should be returned.
        // Any of the described can be changed if neccessary.

        getStorylistContent: function (selection) {
            var self = this;
            $(".personalised-storylist .alert-danger").remove(); // remove error message
            $(".personalised-storylist-content").append('<div class="alert alert-info">Loading content...</div>'); // add loading message
            if (selection == '') {
                var options = $('.dropdown-container li a');
                var names = [];
                $(options).each(function (index) {
                    names.push($.trim($(this).find('input').prop('value'))); // add all categories to array
                });
                selection = names;
            }
            $.ajax({
                url: this.options.baseUrl + "&selection=" + selection + "&length=" + this.options.numberOfStories + "&site=" +this.options.site,
                success: function (result) {
                    jsonresult = jQuery.parseJSON(result);
                    if (jsonresult != '' || typeof jsonresult !== "undefined") { // successfull response
                        $('.personalised-storylist .teaser').removeClass('fade-in'); // hide current content
                        $(".personalised-storylist-content").children().remove(); // remove current content
                        $(jsonresult).each(function (index, teaser) { // for each teaser
                            var even = "";
                            if(index%2 == 0){ // if even
                                even = "even";
                            }
                            // add new content to DOM
                            $(".personalised-storylist-content").append('<div class="headline-teaser-item teaser '+ even +' clearfix"><div class="teaser-title"><h4><span class="teaser-label">'+ teaser['teaser.label'] +': </span><a href="' + teaser['teaser.url'] + '">'+ teaser['teaser.headline'] +'</a><span class="media-icon '+ teaser['teaser.mediaIconClass'] +'"></span></h4><span class="update-time">'+ teaser['teaser.updatedTime'] +'</span></div></div>');
                            setTimeout(function(){
                                $('.personalised-storylist .teaser').addClass('fade-in'); // disaplay new content
                            },10);
                        });
                    } else { // undefined object returned
                        self.setErrorAlert();
                    }
                },
                error: function (xhr, status, error) { // unsuccessfull request
                    self.setErrorAlert();
                }
            });
            $(".personalised-storylist .alert-info").remove(); // remove loading message
        },

        // add error message , to be used when errors occur when retireving data
        setErrorAlert: function () {
            var self = this;
            $(".personalised-storylist-content").append('<div class="alert alert-danger">Could not get new headlines, please try again by <a href="#" class="alert-link">clicking here</a></div>'); // add message to DOM
            $('.personalised-storylist-content .alert-link').on('touchstart click', function (event) { // when 'clicking here' is pressed
                self.getStorylistContent(self.getActiveSeletions()); // refresh content
                return false;
            });
        },
    };

    $.fn.personalisedDropdown = function(option, parameter) {
        return this.each(function() {
            var data = $(this).data('dropdown');
            var options = typeof option === 'object' && option;
            if (!data) { // Initialize the personalisedDropdown.
                $(this).data('dropdown', ( data = new PersonalisedDropdown(this, options)));
            }
            if (typeof option === 'string') { // Call personalisedDropdown method.
                data[option](parameter);
            }
        });
    };

    $.fn.personalisedDropdown.Constructor = PersonalisedDropdown;

}(window.jQuery);