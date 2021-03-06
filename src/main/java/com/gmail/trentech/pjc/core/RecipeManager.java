package com.gmail.trentech.pjc.core;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe.Builder.RowsStep;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe.Builder.ResultStep;
import org.spongepowered.api.item.recipe.smelting.SmeltingRecipe;

import com.gmail.trentech.pjc.utils.InvalidItemTypeException;
import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class RecipeManager {

	public static ConcurrentHashMap<String, ItemStack> customItems = new ConcurrentHashMap<>();
	
	public static ItemStack getItemStack(String item) throws InvalidItemTypeException {
		String[] args = item.split(":");

		Optional<ItemType> optionalItemType = Sponge.getRegistry().getType(ItemType.class, args[0] + ":" + args[1]);

		if (optionalItemType.isPresent()) {
			ItemStack itemStack = ItemStack.builder().itemType(optionalItemType.get()).build();

			if (args.length == 3) {
				DataContainer container = itemStack.toContainer();
				DataQuery query = DataQuery.of('/', "UnsafeDamage");
				container.set(query, Integer.parseInt(args[2]));
				itemStack = Sponge.getDataManager().deserialize(ItemStack.class, container).get();
			}

			return itemStack;
		} else {
			throw new InvalidItemTypeException("ItemType in config.conf at " + item + " is invalid");
		}
	}

	public static ItemType getItemType(String item) throws InvalidItemTypeException {
		Optional<ItemType> optionalItemType = Sponge.getRegistry().getType(ItemType.class, item);

		if (optionalItemType.isPresent()) {
			return optionalItemType.get();
		} else {
			throw new InvalidItemTypeException("ItemType in config.conf at " + item + " is invalid");
		}
	}
	
	public static ShapedCraftingRecipe getShapedRecipe(ConfigurationNode node, ItemStack result) throws InvalidItemTypeException {
		RowsStep rows = ShapedCraftingRecipe.builder().rows();
		
		for(int i = 1; i < 4; i++) {
			String[] row1 = node.getNode("row" + i).getString().split(",");
			
			ArrayList<Ingredient> ingredients = new ArrayList<>();
			
			for(int i1 = 0; i1 < 3; i1++) {
				String item = row1[i1];
				
				if(item.equalsIgnoreCase("NONE")) {
					ingredients.add(Ingredient.builder().with(ItemTypes.NONE).build());
				} else {
					if(customItems.containsKey(item)) {
						ItemStack itemStack = customItems.get(item);
						
						ingredients.add(Ingredient.builder().with(itemStack).withDisplay(itemStack).build());
					} else if(item.split(":").length == 3) {
						ItemStack itemStack = getItemStack(item);
						
						ingredients.add(Ingredient.builder().with(itemStack).withDisplay(itemStack).build());
					} else {
						ItemType itemType = getItemType(item);
						
						ingredients.add(Ingredient.builder().with(itemType).withDisplay(itemType).build());
					}				
				}
			}

			if(i == 3) {
				return rows.row(ingredients.toArray(new Ingredient[0])).result(result).id(node.getNode("id").getString()).build();
			}
			
			rows.row(ingredients.toArray(new Ingredient[0]));
		}
		return null;
	}
	
	public static ShapelessCraftingRecipe getShapelessRecipe(ConfigurationNode node, ItemStack result) throws InvalidItemTypeException {
		try {
			ShapelessCraftingRecipe.Builder builder = ShapelessCraftingRecipe.builder();
			
			ResultStep resultStep = null;
			
			for(String item : node.getNode("ingredients").getList(TypeToken.of(String.class))) {
				if(customItems.containsKey(item)) {
					ItemStack itemStack = customItems.get(item);
					
					resultStep = builder.addIngredient(Ingredient.builder().with(itemStack).withDisplay(itemStack).build());
				} else if(item.split(":").length == 3) {
					ItemStack itemStack = getItemStack(item);
					
					resultStep = builder.addIngredient(Ingredient.builder().with(itemStack).withDisplay(itemStack).build());
				} else {
					ItemType itemType = getItemType(item);
					
					resultStep = builder.addIngredient(Ingredient.builder().with(itemType).withDisplay(itemType).build());
				}				
			}
			
			return resultStep.result(result).id(node.getNode("id").getString()).build();
		} catch (ObjectMappingException e) {
			e.printStackTrace();
			return null;
		}	
	}
	
	public static SmeltingRecipe getSmeltingRecipe(ConfigurationNode node, ItemStack result) throws InvalidItemTypeException {
		String item = node.getNode("ingredient").getString();

		if(item.split(":").length == 3) {
			return SmeltingRecipe.builder().ingredient(getItemStack(item)).result(result).build();
		} else {
			return SmeltingRecipe.builder().ingredient(getItemType(item)).result(result).build();
		}		
	}
}
