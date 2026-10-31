"""Mistborn Allomancy (phase 5): metal flakes, vials and what burning each metal does.

Grind a metal ingot into flakes (alloy flakes are mixed from their parts), add flakes to a glass bottle for a vial,
drink the vial to burn the metal. A player burns one metal at a time; one who ate Lerasium is Mistborn and burns any
number at once. Effects live in Java (com.pipican.ore.Allomancy); this file is the data: names, colours, recipes.
"""

# metal -> (English, Turkish, colour, flakes recipe, burn seconds, what it does)
# flakes recipe: ('ingot', item) = 1 ingot -> 4 flakes; ('mix', [flake ids or items], count) = shapeless mix.
METALS = {
    'iron': ('Iron', 'Demir', 0xD8D8D8, ('ingot', 'minecraft:iron_ingot'), 60,
             'Pulls nearby metal items and metal-armored creatures toward you.'),
    'steel': ('Steel', 'Çelik', 0x9AA4AE, ('mix', ['iron_flakes', 'iron_flakes', ['minecraft:coal', 'minecraft:charcoal']], 2), 60,
              'Pushes on metal: jump in mid-air above metal to launch upward; your arrows hit harder.'),
    'tin': ('Tin', 'Kalay', 0xC9CED3, ('ingot', 'pipican:tin_ingot'), 90,
            'Sharpened senses: night vision, and creatures around you glow.'),
    'pewter': ('Pewter', 'Kalay-Kurşun', 0x8E9599, ('mix', ['tin_flakes', 'tin_flakes', 'iron_flakes'], 3), 60,
               'Strength, toughness and balance.'),
    'copper': ('Copper', 'Bakır', 0xC8743C, ('ingot', 'minecraft:copper_ingot'), 90,
               'A copper cloud: hides your burning from others; creatures notice you later.'),
    'bronze': ('Bronze', 'Bronz', 0xB0803A, ('mix', ['copper_flakes', 'copper_flakes', 'tin_flakes'], 3), 90,
               'Senses other players burning metals nearby.'),
    'zinc': ('Zinc', 'Çinko', 0x9AA5A8, ('ingot', 'pipican:zinc_ingot'), 45,
             'Rioting: hostile creatures around you turn on each other.'),
    'brass': ('Brass', 'Pirinç', 0xD4B04A, ('mix', ['copper_flakes', 'zinc_flakes'], 2), 45,
              'Soothing: creatures around you lose interest in you.'),
    'cadmium': ('Cadmium', 'Kadmiyum', 0xD9C27A, ('ingot', 'pipican:cadmium_ingot'), 30,
                'A slow-time bubble: everything around you is slowed.'),
    'bendalloy': ('Bendalloy', 'Bendalloy', 0xB5A06A, ('mix', ['cadmium_flakes', 'tin_flakes'], 2), 30,
                  'A speed bubble: you and allies nearby move, work and heal fast.'),
    'gold': ('Gold', 'Altın', 0xF2C640, ('ingot', 'minecraft:gold_ingot'), 45,
             'Shows your past: where you last died.'),
    'electrum': ('Electrum', 'Elektrum', 0xE6D47A, ('ingot', 'pipican:electrum_ingot'), 45,
                 'Glimpses of your own future: 25% chance to dodge attacks.'),
    'chromium': ('Chromium', 'Krom', 0xC8D0D8, ('ingot', 'pipican:chromium_ingot'), 45,
                 'Your hits wipe the target: its buffs, and a player\'s burning metals.'),
    'nicrosil': ('Nicrosil', 'Nikrosil', 0xA9B6B2, ('mix', ['nickel_flakes', 'chromium_flakes'], 2), 45,
                 'Touch an ally (right-click) to flare them: strength, speed and longer burns.'),
    'aluminum': ('Aluminum', 'Alüminyum', 0xD5DAE0, ('ingot', 'pipican:aluminum_ingot'), 1,
                 'Drinking it wipes all your burning metals and every effect on you.'),
    'duralumin': ('Duralumin', 'Duralümin', 0xC0C6CC, ('mix', ['aluminum_flakes', 'copper_flakes'], 2), 1,
                  'Drinking it flares everything you burn at triple strength for 5 seconds, then burns it out.'),
    'malatium': ('Malatium', 'Malatium', 0xB0A27A, ('ingot', 'pipican:malatium_ingot'), 60,
                 'Hit a creature to see its past: its weakness, and you deal 20% more damage to it.'),
    'atium': ('Atium', 'Atium', 0x9E9E9E, ('ingot', 'pipican:atium_ingot'), 20,
              'See your foes\' future: 85% chance to dodge attacks.'),
    'trellium': ('Trellium', 'Trellium', 0x7A5C8C, ('ingot', 'pipican:trellium_ingot'), 60,
                 'Hidden from all tracking: creatures farther than 4 blocks can\'t find you.'),
}
# Nickel only goes into Nicrosil: it has flakes but no vial.
FLAKES_ONLY = {'nickel': ('Nickel', 'Nikel', 0xB5B09A, ('ingot', 'pipican:nickel_ingot'))}
