$(document).ready(function () {

  var dataTableInstance = null;

  $('#season').on('change', function () {
    var seasonId = $(this).val();
    $('#league').empty().append('<option disabled="disabled" selected="selected">Loading leagues...</option>');
    var $tableBody = $('#sos-table tbody');

    if ($.fn.DataTable.isDataTable('#sos-table')) {
      dataTableInstance.destroy();
    }
    $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="3">Select a league to continue.</td></tr>');

    $.get("/seasons/" + seasonId + "/leagues", function (data) {
      var $leagueDropdown = $('#league');
      $leagueDropdown.empty().append('<option disabled="disabled" selected="selected">Select a League</option>');
      $.each(data, function (index, league) {
        $leagueDropdown.append($('<option>', {
          value: league.id,
          text: league.name
        }));
      });
    });
  });

  $('#league').on('change', function () {
    var leagueId = $(this).val();
    var $tableBody = $('#sos-table tbody');

    if ($.fn.DataTable.isDataTable('#sos-table')) {
      dataTableInstance.destroy();
    }

    $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="3">Calculating Strength of Schedule... Please wait.</td></tr>');

    $.get("/leagues/" + leagueId + "/sos-info", function (data) {

      var teams = data.teams;

      teams.sort(function (a, b) {
        var scoreA = a.strengthOfSchedule || 0;
        var scoreB = b.strengthOfSchedule || 0;
        return scoreB - scoreA;
      });

      $tableBody.empty();

      if (!teams || teams.length === 0) {
        $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="3">No data found for this league.</td></tr>');
        return;
      }

      $.each(teams, function (index, team) {

        var rowClass = '';

        var sosScore = team.strengthOfSchedule;
        var scoreDisplay = ".000";

        if (sosScore != null && sosScore > 0) {
          scoreDisplay = sosScore.toFixed(3);
          if (scoreDisplay.startsWith("0")) {
            scoreDisplay = scoreDisplay.substring(1);
          }
        } else {
          scoreDisplay = "N/A (Playoff/Season End)";
        }

        if (sosScore > 0) {
          if (index === 0) { rowClass = 'bg-red-500/30 text-red-200'; }
          else if (index === 1) { rowClass = 'bg-red-500/20 text-red-200'; }
          else if (index === 2) { rowClass = 'bg-red-500/10 text-red-200'; }
          else if (index === teams.length - 1) { rowClass = 'bg-emerald-500/30 text-emerald-200'; }
          else if (index === teams.length - 2) { rowClass = 'bg-emerald-500/20 text-emerald-200'; }
          else if (index === teams.length - 3) { rowClass = 'bg-emerald-500/10 text-emerald-200'; }
          else { rowClass = 'bg-transparent text-slate-300'; }
        } else {
          rowClass = 'bg-slate-800/30 text-slate-500 italic';
        }

        var row =
          `<tr class="${rowClass}">
            <td class="px-4 py-3">${index + 1}</td>
            <td class="px-4 py-3 font-medium text-white">${team.name}</td>
            <td class="px-4 py-3 font-bold">${scoreDisplay}</td>
          </tr>`;

        $tableBody.append(row);
      });


      dataTableInstance = $('#sos-table').DataTable({
        "paging": false,
        "info": false,
        "autoWidth": false,
        "order": [[2, "desc"]]
      });
    });
  });

});
