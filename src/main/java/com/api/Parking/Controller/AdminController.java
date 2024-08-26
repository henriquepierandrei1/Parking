package com.api.Parking.Controller;

import com.api.Parking.Dto.CreateParkedDto;
import com.api.Parking.Model.CarModel;
import com.api.Parking.Model.ParkedModel;
import com.api.Parking.Service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@ApiResponse()
public class AdminController {
    private final AdminService adminService;


    @GetMapping("/parkeds")
    @Operation(summary = "Obter todos os veículos estacionados, de acordo com a data atual!", description = "Retorna as informações do veículo e a vaga estacionado.")
    public ResponseEntity<List<?>> getParkeds(@RequestParam(value = "date") String date) {
        try {
            // Obtem e retorna a entidade salva no Banco de Dados!
            List<?> parkeds = this.adminService.getParkedByDate(date);
            return ResponseEntity.ok(parkeds);
        } catch (Exception e) {
            // Caso há algum erro, retornar um ERROR 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }



    @PostMapping("/create")
    @Operation(summary = "Registrar veículo no estacionamento!")
    public ResponseEntity<?> createParked(@RequestBody CreateParkedDto createParkedDto){

        // Verifica se já existe algum carro na vaga!
        Optional<ParkedModel> optionalParkedModel = this.adminService.getByPlace(createParkedDto.place());
        if (optionalParkedModel.isPresent()){
            // Se existe, retornar uma mensagem dizendo que está indisponível a vaga para estacionamento!
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not available!");
        }

        // Cria a entidade CarModel para armazenar no ParkedModel e salvando no Banco de Dados.
        CarModel model = new CarModel();
        model.setCarModel(createParkedDto.car().getCarModel());
        model.setCarMark(createParkedDto.car().getCarMark());
        model.setPlate(createParkedDto.car().getPlate());
        model.setColor(createParkedDto.car().getColor());
        this.adminService.saveCar(model);


        // Criando ParkedModel e salvando no Banco de Dados.
        ParkedModel parkedModel = new ParkedModel();
        parkedModel.setDateTimeArrival(LocalDateTime.now().toString());
        parkedModel.setPlace(createParkedDto.place().toString());
        parkedModel.setCode(this.adminService.createCode());
        parkedModel.setCar(model);
        this.adminService.saveParked(parkedModel);


        return ResponseEntity.status(HttpStatus.CREATED).body(parkedModel);

    }


    @Operation(summary = "Obtém o valor a pagar de acordo com o tempo estacionado!", description = "Retorna o valor em reais!")
    @GetMapping("/parking/difference")
    public ResponseEntity<Object> finishParkingPerPlace(@RequestParam(value = "code") String code){
        // Obtém a entidade de acordo com o seu código de acesso!
        Optional<ParkedModel> optionalParkedModel = this.adminService.getByCode(code);

        // Se a entidade for encontrada, cai nessa condição!
        if (optionalParkedModel.isPresent()){

            // Recebe o valor a pagar!
            String value = this.adminService.hourDifferent(LocalDateTime.parse(optionalParkedModel.get().getDateTimeArrival()),5);
            return ResponseEntity.status(HttpStatus.FOUND).body(value);
        }
        // Caso não ache a entidade, retorna um erro!
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND!");
    }



    @DeleteMapping("/parking/delete")
    @Operation(summary = "Deleta o veiculo da vaga estacionada, podendo outro veículo usá-la!")
    public Object paidPark(@RequestParam(value = "code") String code){
        // Obtém a entidade pelo código de acesso!
        Optional<ParkedModel> optionalParkedModel = this.adminService.getByCode(code);

        // Verifica se foi encontrado, se sim ele cai na condição!
        if (optionalParkedModel.isPresent()){
            // Deleta os dados do banco e retornar a vaga que foi liberada!
            this.adminService.deleteParkedAndCarModel(optionalParkedModel.get(),optionalParkedModel.get().getCar());
            return ResponseEntity.status(HttpStatus.OK).body("Place: "+optionalParkedModel.get().getPlace()+" disponível!");
        }

        // Caso não encontra, ele retornar um erro NOT FOUND!
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found!");

    }



}
