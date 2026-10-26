# RPG Overworld height

The custom world preset now references its own dimension type and noise settings with min_y=-64, height=576, logical_height=576. The resulting nominal build range is Y=-64..511. Vanilla Overworld saves remain untouched. This change requires fresh save creation and CI/server worldgen validation. Vanilla noise functions may remain too flat near the newly extended top; custom mountain/capital geology still needs a tailored density function.
