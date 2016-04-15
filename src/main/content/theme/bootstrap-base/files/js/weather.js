//Used to sort issues with IE8 and console.logs
function emptyFunction(a) {
}
window.console = window.console || {
  log : emptyFunction,
  info : emptyFunction,
  warn : emptyFunction,
  error : emptyFunction
}

// Global variable
var defaultLocation = 'W1'; // user can set this in Polopoly - should be W1
// default

// When doc loaded - run
$(function() {

  // Check to see if the user is logged in or not?
  if (readCookie("sessionKey") != false) {
    // User logged in - grab preferred stored post code.
    console.log('user logged in');
    
    var postcode = qs('postcode');
    
    // in case the postcode exist in query string, use it instead
    if (postcode != null && postcode != undefined && postcode.length > 0) {
      getWeatherForecast(postcode);
    } else {
      if (typeof (localStorage.todayTemp) !== "undefined" && localStorage.override === "true") {
        if (!getStoredWeather()) {
          getUserWeather();
        }
      } else {
        getUserWeather();
      }
    }
  } else {
  	if (localStorage.override === "true") localStorage.override = false;
    getStoredWeather();
  }
}); // End doc ready

/*
 * ----------------------------------------------------- FUNCTIONS
 * ///////////////////////////////////////////
 */

function getUserWeather() {
  $.ajax({
    dataType: "json",
    url: '/ajax?action=loginStatus',
    success: function(json) {
      if(json.loginStatus.postCode != null) {
        getWeatherForecast(json.loginStatus.postCode);
      } else {
        getStoredWeather();
      }
    },
    error: function() {
      getStoredWeather();
    }
  });
}

function getStoredWeather() {
  if (typeof (localStorage.todayTemp) !== "undefined") {

    // Yes, localStorage supported - check how old data is?
    var todaysTime = new Date().getTime();
    var localStorageTime = localStorage.timeFetched;
    if (todaysTime < localStorageTime) {

      // date is not too old, retrieve from localStorage.
      console.log('LOCAL STORAGE');

      $('.weather-header .location').html(localStorage.weatherLocation).show();
      $('.temperature.current-climate').html(localStorage.todayTemp + "&deg;").show();
      $('.weather-header .weather-image').attr("src", localStorage.weatherImage).show();
      
      // set 5-day forecast link to url with specified location
      var pageUrl = $('.weather-header').data('weather-page');
      resetWeatherUrl(pageUrl, localStorage.weatherLocation);
      
      // set modal input text to the new location name
      resetWeatherLocation(localStorage.weatherLocation);

      return true;
    }
  }

  // No localStorage variables
  var preferedLocation = $('.weather-header').data('preffered-location');
  getWeather(preferedLocation);
  
  return false;
}

function getWeatherForecast(locationName) {
  getWeather(locationName);
  if (typeof (localStorage.todayTemp) !== "undefined") {
    localStorage.override = true;
  }
}

function getWeather(locationName) {
  console.log('locationName first: ' + locationName);

  if (typeof (locationName) == "undefined") {
    // if locationName not passed to function
    // use default location
    console.log('using cookie value: ' + defaultLocation);
    ajaxWeather(defaultLocation);

  } else { // argument passed - use this variable

    console.log('using form value: ' + locationName);
    var isLocationNameSterile = regexPostCode(locationName);

    if (isLocationNameSterile == true) {
      // passed regex - hide any previous error messages
      $('.weather-header .no-weather').addClass('hide');
      ajaxWeather(locationName);
    } else {
      // regex failed - show message
      $('.weather-header .no-weather').html(
          'Please enter a valid UK postcode - thank you');
      $('.weather-header .hide.no-weather').removeClass('hide');
    }
  }
}

function regexPostCode(location) {

  // Therefor using simple letters / numbers regex.
  var postCodeReg = /^[A-Za-z0-9 ]*[A-Za-z0-9][A-Za-z0-9 ]*$/;

  if (!postCodeReg.test(location)) {
    // Failed regex
    console.log('failed regex');
    return false;
  } else {
    // Passed Regex
    console.log('passed regex');
    return true;
  }
}

function ajaxWeather(locationName) {
  console.log('ajax: ' + locationName);
  console.log('ajaxWeather() ran');
  
  var contentUrl = $('.weather-header').data('content-url');
  
  if (contentUrl == undefined || contentUrl == null) return;

  //var url = "http://www.edp24.co.uk/news/weather?contentonly=true&ajax=true&submit=true&postcode=" + locationName;
  var url = contentUrl;
  var ot = 'archant.AjaxPageLayout.ot';
  
  $.ajax({
    // URL for fetching weather and variable name for location/post code
    // (this may need edit depending on the url string needed).
    url : url,
    data: {
      "ot" : ot,
      "isAjaxView" : "true",
      "postcode" : locationName
    },
    dataType: "json",
    success : function(result) {

      // object result might still be successful, but not have anything in
      // it.
      // so check for object we use.
      if (typeof (result.todayTemp) !== "undefined") { // Unsure of what
        // exact object name
        // will be....

        // add result to the DOM
        $('.weather-header .location').html(locationName).show();
        $('.temperature.current-climate').html(result.todayTemp + "&deg;").show();
        $('.weather-header .weather-image')
            .attr("src", result.weatherImage).show();

        // set result in localStorage
        localStorage.todayTemp = result.todayTemp;
        localStorage.weatherLocation = locationName;
        localStorage.weatherImage = result.weatherImage;

        // add 30minutes to time to effect 'cache timeout'
        localStorage.timeFetched = new Date().getTime() + 30 * 60000;
        
        // set 5-day forecast link to url with specified location
        var pageUrl = $('.weather-header').data('weather-page');
        resetWeatherUrl(pageUrl, locationName);
        
        // set modal input text to the new location name
        resetWeatherLocation(locationName);
        
        var win = $('#my-weather-modal');
        if (win.hasClass('in')) {
          win.modal('hide');
        }

      } else {
        // aJax didn't return to us a useable object - display error
        // message.
        $('.weather-header .hide.no-weather').html(
            "There was an error with your request, please try again.");
      }

    },
    error : function(xhr, status, error) {
      // Error handling messages
      $('.weather-header .hide.no-weather')
          .html(
              "We do not have a forecast for "
                  + locationName
                  + ". <br> <span class='secondary-message'>Please ensure you enter a valid postcode and try again</span>");

      $('.weather-header .hide.no-weather').removeClass('hide');
      console.log('Ajax no results, error: ');
      console.log('xhr: ' + xhr);
      console.log('status: ' + status);
      console.log('error: ' + error);
    }
  });
}

function readCookie(cookieName) {
  var re = new RegExp('[; ]' + cookieName + '=([^\\s;]*)');
  var sMatch = (' ' + document.cookie).match(re);

  if (cookieName && sMatch) {
    return unescape(sMatch[1]);
  } else {
    return false;
  }
}

function resetWeatherUrl(pageUrl, locationName) {
  var url = pageUrl + "?postcode=" + locationName;
  $('.weather-header .forecast-link').attr('href', url).show();
}

function resetWeatherLocation(locationName) {
  $('#weather-location input:text[name=location]').val(locationName);
}

function qs(name) {
  var query = window.location.search.substring(1);
  var vars = query.split("&");
  for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split("=");
    if(pair[0] == name){return pair[1];}
  }
  return null;
}