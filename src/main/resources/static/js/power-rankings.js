$(document).ready(function () {

  var dataTableInstance = null;

  $('#season').on('change', function () {
    var seasonId = $(this).val();
    $('#league').empty().append('<option disabled="disabled" selected="selected">Loading leagues...</option>');
    var $tableBody = $('#pr-table tbody');

    if ($.fn.DataTable.isDataTable('#pr-table')) {
      dataTableInstance.destroy();
    }
    $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="4">Select a league to continue.</td></tr>');

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
    var $tableBody = $('#pr-table tbody');

    if ($.fn.DataTable.isDataTable('#pr-table')) {
      dataTableInstance.destroy();
    }

    $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="4">Calculating Power Rankings... Please wait.</td></tr>');

    $.get("/leagues/" + leagueId + "/power-rankings", function (data) {
      var teams = data;
      $tableBody.empty();

      if (!teams || teams.length === 0) {
        $tableBody.html('<tr class="bg-slate-800/30 text-slate-400 text-center"><td colspan="4">No data found for this league.</td></tr>');
        return;
      }

      var lastWinRateStr = "";
      var currentDisplayRank = 0;

      $.each(teams, function (index, team) {

        var rowClass = '';

        var winRate = team.winRate;
        var winRateStr = winRate ? winRate.toFixed(3) : "0.000";
        if (winRateStr.startsWith("0")) {
          winRateStr = winRateStr.substring(1);
        }

        // Tie Handling
        if (index === 0 || winRateStr !== lastWinRateStr) {
          currentDisplayRank = index + 1;
        }
        lastWinRateStr = winRateStr;

        var won = team.totalCategoriesWon;
        var tied = team.totalCategoriesTied;
        var played = team.totalCategoriesPlayed;
        var lost = played - won - tied;
        var recordString = `${won} / ${Math.round(lost)} / ${tied}`;

        if (index === 0) { rowClass = 'bg-emerald-500/30 text-emerald-200'; }
        else if (index === 1) { rowClass = 'bg-emerald-500/20 text-emerald-200'; }
        else if (index === 2) { rowClass = 'bg-emerald-500/10 text-emerald-200'; }
        else if (index === teams.length - 1) { rowClass = 'bg-red-500/30 text-red-200'; }
        else if (index === teams.length - 2) { rowClass = 'bg-red-500/20 text-red-200'; }
        else if (index === teams.length - 3) { rowClass = 'bg-red-500/10 text-red-200'; }
        else { rowClass = 'transition-colors hover:bg-slate-800/50 text-slate-300'; }

        var row =
          `<tr class="${rowClass}">
            <td class="px-4 py-3">${currentDisplayRank}</td>
            <td class="px-4 py-3 font-medium text-white">${team.name}</td>
            <td class="px-4 py-3 font-bold">${winRateStr}</td>
            <td class="px-4 py-3">${recordString}</td>
          </tr>`;

        $tableBody.append(row);
      });

      dataTableInstance = $('#pr-table').DataTable({
        "paging": false,
        "info": false,
        "autoWidth": false,
        "order": [[0, "asc"]]
      });
    });
  });

});
