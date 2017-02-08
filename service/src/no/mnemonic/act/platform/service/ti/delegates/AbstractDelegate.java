package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.entity.cassandra.Direction;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiRequestContext;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The AbstractDelegate provides common methods used by multiple delegates.
 */
abstract class AbstractDelegate {

  /**
   * Fetch an existing FactType by ID.
   *
   * @param id UUID of FactType
   * @return Existing FactType
   * @throws ObjectNotFoundException Thrown if FactType cannot be found
   */
  FactTypeEntity fetchExistingFactType(UUID id) throws ObjectNotFoundException {
    FactTypeEntity entity = TiRequestContext.get().getFactManager().getFactType(id);
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("FactType with id = %s does not exist.", id));
    }
    return entity;
  }

  /**
   * Fetch an existing ObjectType by ID.
   *
   * @param id UUID of ObjectType
   * @return Existing ObjectType
   * @throws ObjectNotFoundException Thrown if ObjectType cannot be found
   */
  ObjectTypeEntity fetchExistingObjectType(UUID id) throws ObjectNotFoundException {
    ObjectTypeEntity entity = TiRequestContext.get().getObjectManager().getObjectType(id);
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("ObjectType with id = %s does not exist.", id));
    }
    return entity;
  }

  /**
   * Assert that a FactType does not yet exist (by name).
   *
   * @param name Name of FactType
   * @throws InvalidArgumentException Thrown if FactType already exists
   */
  void assertFactTypeNotExists(String name) throws InvalidArgumentException {
    if (TiRequestContext.get().getFactManager().getFactType(name) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("FactType with name = %s already exists.", name), "fact.type.exist", "name", name);
    }
  }

  /**
   * Assert that an ObjectType does not yet exist (by name).
   *
   * @param name Name of ObjectType
   * @throws InvalidArgumentException Thrown if ObjectType already exists
   */
  void assertObjectTypeNotExists(String name) throws InvalidArgumentException {
    if (TiRequestContext.get().getObjectManager().getObjectType(name) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("ObjectType with name = %s already exists.", name), "object.type.exist", "name", name);
    }
  }

  /**
   * Assert that ObjectTypes exist when a binding definition between a FactType and an ObjectType should be created.
   *
   * @param bindingDefinitions FactType/ObjectType binding definitions
   * @param propertyName       Property name
   * @throws InvalidArgumentException Thrown if an ObjectType part of a binding definition does not exist
   */
  void assertObjectTypesToBindExist(Collection<FactObjectBindingDefinition> bindingDefinitions, String propertyName) throws InvalidArgumentException {
    boolean invalid = false;
    InvalidArgumentException ex = new InvalidArgumentException();

    for (FactObjectBindingDefinition definition : bindingDefinitions) {
      if (TiRequestContext.get().getObjectManager().getObjectType(definition.getObjectType()) == null) {
        ex.addValidationError(String.format("ObjectType with id = %s does not exist.", definition.getObjectType()),
                "object.type.not.exist", propertyName, definition.getObjectType().toString());
        invalid = true;
      }
    }

    if (invalid) {
      throw ex;
    }
  }

  /**
   * Assert that an EntityHandler exists.
   *
   * @param entityHandler          Name of EntityHandler
   * @param entityHandlerParameter Parameter of EntityHandler
   * @throws InvalidArgumentException Thrown if EntityHandler does not exist
   */
  void assertEntityHandlerExists(String entityHandler, String entityHandlerParameter) throws InvalidArgumentException {
    try {
      TiRequestContext.get().getEntityHandlerFactory().get(entityHandler, entityHandlerParameter);
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if an EntityHandler cannot be found.
      throw new InvalidArgumentException()
              .addValidationError(ex.getMessage(), "entity.handler.not.exist", "entityHandler", entityHandler);
    }
  }

  /**
   * Assert that a Validator exists.
   *
   * @param validator          Name of Validator
   * @param validatorParameter Parameter of Validator
   * @throws InvalidArgumentException Thrown if Validator does not exist
   */
  void assertValidatorExists(String validator, String validatorParameter) throws InvalidArgumentException {
    try {
      TiRequestContext.get().getValidatorFactory().get(validator, validatorParameter);
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if a Validator cannot be found.
      throw new InvalidArgumentException()
              .addValidationError(ex.getMessage(), "validator.not.exist", "validator", validator);
    }
  }

  /**
   * Convert FactObjectBindingDefinitions from a request to entities.
   *
   * @param bindingDefinitions Definitions as part of a request
   * @return Definitions converted to entities
   */
  List<FactTypeEntity.FactObjectBindingDefinition> convertFactObjectBindingDefinitions(List<FactObjectBindingDefinition> bindingDefinitions) {
    return bindingDefinitions.stream()
            .map(r -> new FactTypeEntity.FactObjectBindingDefinition()
                    .setObjectTypeID(r.getObjectType())
                    .setDirection(Direction.valueOf(r.getDirection().name())))
            .collect(Collectors.toList());
  }

}
