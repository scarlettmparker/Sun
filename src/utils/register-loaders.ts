/**
 * Registers all page data loaders via their colocated data modules.
 */

import "~/routes/blog/blog-data";
import "~/routes/blog/[id]/blog-post-data";
import "~/routes/gallery/gallery-data";
import "~/routes/stem-player/stem-player-data";
import "~/routes/stem-player/[id]/stem-player-details-data";
import "~/server/hub/hub-registrations";
