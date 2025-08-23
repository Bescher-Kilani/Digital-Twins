package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.UploadException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.validation.AASModelValidator;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;

import java.io.IOException;
import java.util.List;

/**
 * Handles uploading and validating AAS models from JSON files.
 */
@Service
@RequiredArgsConstructor
public class AASModelUploadService {

    private final AASModelValidator aasModelValidator;
    private final AASModelMapper aasModelMapper;
    private final FileUploadValidator fileUploadValidator;

    /**
     * Uploads a JSON file containing an AAS environment and returns a validated model DTO.
     *
     * @param file    uploaded multipart JSON file
     * @param ownerId ID of the uploading user
     * @return validated model as DTO
     * @throws UploadException      on file read or parse failure
     * @throws ValidationException  on model structure validation failure
     */
    public AASModelDto uploadAASModel(MultipartFile file, String ownerId) {

            fileUploadValidator.validate(file);


        try {
            // Step 1: parse the JSON content into a full AAS environment
            Environment environment = parseEnvironment(file);

            // Step 2: extract AAS and Submodels
            AssetAdministrationShell shell = environment.getAssetAdministrationShells().stream()
                    .findFirst()
                    .orElseThrow(() -> new UploadException("No AssetAdministrationShell found in uploaded file"));

            if (!(shell instanceof DefaultAssetAdministrationShell aas)) {
                throw new UploadException("Expected DefaultAssetAdministrationShell but found: " + shell.getClass().getSimpleName());
            }


            // Ensure all submodels are concrete DefaultSubmodel instances
            // and fail early if an unexpected implementation is found
            List<DefaultSubmodel> submodels = environment.getSubmodels().stream()
                    .map(sm -> {
                        if (sm instanceof DefaultSubmodel) return (DefaultSubmodel) sm;
                        else throw new UploadException("Invalid submodel type: " + sm.getClass().getSimpleName());
                    })
                    .toList();


            // Step 3: build the domain model
            AASModel model = AASModel.builder()
                    .aas(aas)
                    .submodels(submodels)
                    .ownerId(ownerId)
                    .published(false)
                    .publishMetadata(new PublishMetadata())
                    .build();

            // Step 4: validate model structure and rules
            aasModelValidator.validate(model);

            // Step 5: convert to DTO for response
            return aasModelMapper.toDto(model);

        } catch (DeserializationException | IOException e) {
            throw new UploadException("Failed to parse uploaded AAS JSON file", e);
        } catch (ValidationException e) {
            throw new BadRequestException("File validation failed", e);
        } catch (Exception e) {
            throw new UploadException("Unexpected error during upload", e);
        }
    }

    //Parses the MultipartFile into an AAS Environment (JSON format).
    public Environment parseEnvironment(MultipartFile file) throws IOException, DeserializationException {
        JsonDeserializer deserializer = new JsonDeserializer();
        return deserializer.read(file.getInputStream(), Environment.class);
    }
}
