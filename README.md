# In short
TrainsInSpace (name pending) is a train management sim set on other planets.
The game revolves around maintaining a high-performance cargo distribution network and reaching preset goals.

# Core game mechanics
Tracks are constructed as a concatenation of circle sections and straight pieces.
Tracks are not limited nor snapped to a grid, and can be built high above ground.
Initially, the game will _not_ support tunnels.
Tracks exist in various track types. Some trains require specific types of track, and track types may provide bonuses.
Building track cost funds, and destroying tracks refunds most of the build price. 
Each track type has a unique price and may use a different price calculation.
On tracks, one can build signals. Signals provide path finding for trains moving in the signal direction, and trains may stop at these signals if no path is available.
In opposite direction, the behaviour of the signal can be toggled between blocking and open. The open option does not provide path finding.
Stations can be built freely as well. These stations contain a number of parallel straight track pieces of equal length.

A train consists of a number of locomotives and a number of wagons.
All train elements have a build price, maintenance cost, resistances, weight and accepted track types.
Wagons, and some specialized locomotives, can carry cargo.
Each wagon has for each cargo type a specific amount it can carry.
Trains can be constructed, bought and sold at a station. New trains are initially stored inside the station. 
Any train can be stored in a station and from storage be placed on any track of that station.
They can not be transferred between stations, and selling a train refunds the build price minus the lifetime profit, down to a reasonable minimum.

An industry provides and accepts cargo.
These can be found on predefined places on a map, and in some situations the player can fund an industry on a predefined place.
When a station is built in range of an industry, the station starts to accept cargo that is accepted by the industry.
The industry distributes its produced cargo to any station in range.

Industries have a passive flow of cargo that is invisible to the player.
The result is that industries do not always provide maximum cargo to connected stations and industries that converts cargo do provide a little passive output.
Industries have an internal cargo capacity, and when the industry produces more than the capacity may contain, the remainder is lost (to the player).
Industries also have a processing speed, such that some industries have a maximum accepted cargo flow. 
Any cargo above the capacity pays far below the usual sales price.

# Core gameplay
The player will spend most of his time looking at aggregated profit information, trying to optimize the different lines that it employs.
He can set a template for each of its lines to automatically edit trains as they enter the station and can move trains between lines as needed.

The player analyses its current economic situation to find what opportunities there are.
It adds trains to existing lines, adds new lines to its network or builds a new station and connection.
Alternatively, the player removes a section of track, and replaces it with a better performing network.

# Setting
The game plays in the far future, at the start of the space-faring era.
Pioneering companies have established industries on faraway planets, using tractor beams and orbital space ships for supplies and service.

The player is a new employee of a transportation company, that specializes in railroad-like transportation for off-world companies.
The player starts each game by picking a contract, after which he has to create an infrastructure that meets the client's preset goals.
The company is shown to be incredibly aggressive, very short-sighted, and will never decline a contract.
Contract ideas include planets on a time-limit due to an impending supernova or a planet at war requiring infrastructure.
The CEO changes every so-often, causing groups of scenarios to have different mechanics and bonuses.

Not all elements of the game are available at the start, and not all previously available elements are available either.
There is no tech research during a scenario, as quality is a matter of pricing.
The R&D department appears to be just two guys (Robert and Dave), one of them being sane.

Tracks are monorail-like, and trains have acceleration mechanics resembling electric propulsion.
Trains generally look sturdy, mechanical and not necessarily futuristic. 
Train can run on any reasonable fuel type, including coal, but each type should have a similar theme/art style.
