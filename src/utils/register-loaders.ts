/**
 * Registers all page data loaders.
 * TODO: Have this as a point of registering data loaders and have specific
 * stem-player etc. data loaders registered here.
 */

import { registerBlogDataLoader } from "~/routes/blog/blog";
import { registerStemPlayerDetailsDataLoader } from "~/routes/stem-player/[id]/stem-player-details";
import { registerStemPlayerDataLoader } from "~/routes/stem-player/stem-player";

// Register all loaders
registerBlogDataLoader();
registerStemPlayerDataLoader();
registerStemPlayerDetailsDataLoader();
